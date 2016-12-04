package org.cobbzilla.wizardtest.resources;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerConfigurationFilter;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.RestServerLifecycleListener;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.string.StringUtil.camelCaseToSnakeCase;
import static org.cobbzilla.util.string.StringUtil.truncate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// initialize a new one. parallel tests should share the same server, but user a different api client.
@Slf4j
public abstract class AbstractResourceIT<C extends RestServerConfiguration, S extends RestServer<C>>
        implements RestServerLifecycleListener<C>, RestServerConfigurationFilter<C> {

    @Getter private final ApiClientBase api = new BasicTestApiClient();

    public void setToken(String sessionId) { getApi().setToken(sessionId); }
    public void pushToken(String sessionId) { getApi().pushToken(sessionId); }
    public void setCaptureHeaders(boolean capture) { getApi().setCaptureHeaders(capture); }
    public void logout() { getApi().logout(); }

    public RestResponse get(String url) throws Exception { return getApi().get(url); }
    public RestResponse doGet(String url) throws Exception { return getApi().doGet(url); }

    public RestResponse put(String url, String json) throws Exception { return getApi().put(url, json); }
    public <T> T put(String url, T o) throws Exception { return getApi().put(url, o); }
    public <T> T put(String path, Object request, Class<T> responseClass) throws Exception { return getApi().put(path, request, responseClass); }
    public RestResponse doPut(String uri, String json) throws Exception { return getApi().doPut(uri, json); }

    public RestResponse post(String url, String json) throws Exception { return getApi().post(url, json); }
    public <T> T post(String url, T o) throws Exception { return getApi().post(url, o); }
    public <T> T post(String path, Object request, Class<T> responseClass) throws Exception { return getApi().post(path, request, responseClass); }
    public RestResponse doPost(String uri, String json) throws Exception { return getApi().doPost(uri, json); }

    public RestResponse delete(String url) throws Exception { return getApi().delete(url); }
    public RestResponse doDelete(String url) throws Exception { return getApi().doDelete(url); }

    protected abstract List<ConfigurationSource> getConfigurations();
    protected List<ConfigurationSource> getConfigurationSources(String... paths) {
        return StreamConfigurationSource.fromResources(getClass(), paths);
    }

    protected Class<? extends S> getRestServerClass() { return getFirstTypeParam(getClass(), RestServer.class); }

    @Override public C filterConfiguration(C configuration) { return configuration; }

    @Override public void beforeStart(RestServer<C> server) {}
    @Override public void onStart(RestServer<C> server) {
        final RestServerConfiguration config = serverHarness.getConfiguration();
        config.setPublicUriBase("http://127.0.0.1:" +config.getHttp().getPort()+"/");
    }
    @Override public void beforeStop(RestServer<C> server) {}
    @Override public void onStop(RestServer<C> server) {}
    protected static RestServerHarness<? extends RestServerConfiguration, ? extends RestServer> serverHarness = null;

    protected static volatile RestServer server = null;

    protected static <T> T getBean(Class<T> beanClass) { return server.getApplicationContext().getBean(beanClass); }

    protected C getConfiguration () { return (C) server.getConfiguration(); }

    public boolean shouldCacheServer () { return true; }
    public boolean useTestSpecificDatabase () { return false; }

    @Before public synchronized void startServer() throws Exception {
        if (serverHarness == null || server == null || !shouldCacheServer()) {
            if (server != null) server.stopServer();
            serverHarness = new RestServerHarness<>(getRestServerClass());
            serverHarness.setConfigurations(getConfigurations());
            serverHarness.addConfigurationFilter(this);
            serverHarness.init(getServerEnvironment());
            server = serverHarness.getServer();
            if (useTestSpecificDatabase() && server.getConfiguration() instanceof HasDatabaseConfiguration) initTestDb();
            server.addLifecycleListener(this);
            serverHarness.startServer();
        }
    }

    private static final Map<String, AbstractResourceIT> dbNames = new ConcurrentHashMap<>();
    protected void createDb(String dbName) throws IOException { notSupported("createDb: must be defined in subclass"); }
    protected void dropDb(String dbName) throws IOException { notSupported("dropDb: must be defined in subclass"); }

    public void initTestDb() throws IOException {
        final DatabaseConfiguration database = ((HasDatabaseConfiguration) server.getConfiguration()).getDatabase();
        String url = database.getUrl();
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash == -1 || lastSlash == url.length()-1) {
            log.warn("initTestDb: couldn't understand url: "+url+", leaving as is");
            return;
        }
        final String dbName = truncate(url.substring(lastSlash+1), 15) + "_" + truncate(camelCaseToSnakeCase(getClass().getSimpleName()), 35) + "_" + randomAlphanumeric(8);
        dropDb(dbName);
        createDb(dbName);
        synchronized (dbNames) { dbNames.put(dbName, this); }
        database.setUrl(url.substring(0, lastSlash)+"/"+dbName);
    }

    @AfterClass public static void cleanupTempDatabases () {
        synchronized (dbNames) {
            final Set<String> removed = new HashSet<>();
            for (Map.Entry<String, AbstractResourceIT> entry : dbNames.entrySet()) {
                final String dbName = entry.getKey();
                final AbstractResourceIT test = entry.getValue();
                try {
                    test.dropDb(dbName);
                    removed.add(dbName);
                } catch (IOException e) {
                    log.warn("Error dropping database: " + dbName + ": " + e, e);
                }
            }
            for (String db : removed) dbNames.remove(db);
        }
    }

    protected Map<String, String> getServerEnvironment() throws Exception { return null; }

    @After public void stopServer () throws Exception {
        if (server != null && !shouldCacheServer()) {
            server.stopServer();
            server = null;
        }
    }

    protected Map<String, ConstraintViolationBean> mapViolations(ConstraintViolationBean[] violations) {
        final Map<String, ConstraintViolationBean> map = new HashMap<>(violations == null ? 1 : violations.length);
        for (ConstraintViolationBean violation : violations) {
            map.put(violation.getMessageTemplate(), violation);
        }
        return map;
    }

    protected void assertExpectedViolations(RestResponse response, String... violationMessages) throws Exception{
        assertEquals(HttpStatusCodes.UNPROCESSABLE_ENTITY, response.status);
        final ConstraintViolationBean[] violations = JsonUtil.FULL_MAPPER.readValue(response.json, ConstraintViolationBean[].class);
        assertExpectedViolations(violations, violationMessages);
    }

    protected void assertExpectedViolations(Collection<ConstraintViolationBean> violations, String... violationMessages) {
        assertExpectedViolations(violations.toArray(new ConstraintViolationBean[violations.size()]), violationMessages);
    }

    protected void assertExpectedViolations(ConstraintViolationBean[] violations, String... violationMessages) {
        final Map<String, ConstraintViolationBean> map = mapViolations(violations);
        assertEquals(violationMessages.length, map.size());
        for (String message : violationMessages) {
            assertTrue("assertExpectedViolations: key "+message+" not found in map: "+map, map.containsKey(message));
        }
    }

    protected ResultSetBean execSql(String sql, Object... args) throws Exception {
        return getConfiguration().execSql(sql, args);
    }

}
