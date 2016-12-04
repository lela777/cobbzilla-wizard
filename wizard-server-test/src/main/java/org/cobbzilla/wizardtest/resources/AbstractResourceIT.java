package org.cobbzilla.wizardtest.resources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.Sleep;
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
import org.junit.Before;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.daemon;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
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

    @Override public void onStart(RestServer<C> server) {
        final RestServerConfiguration config = serverHarness.getConfiguration();
        config.setPublicUriBase("http://127.0.0.1:" +config.getHttp().getPort()+"/");
    }
    @Override public void beforeStop(RestServer<C> server) {}
    protected static RestServerHarness<? extends RestServerConfiguration, ? extends RestServer> serverHarness = null;

    protected static volatile RestServer server = null;

    protected static <T> T getBean(Class<T> beanClass) { return server.getApplicationContext().getBean(beanClass); }

    protected C getConfiguration () { return (C) server.getConfiguration(); }

    public boolean shouldCacheServer () { return true; }
    public boolean useTestSpecificDatabase () { return false; }

    @Before public synchronized void startServer() throws Exception {
        if (serverHarness == null || server == null || !shouldCacheServer() || requireNewServer(server)) {
            if (server != null) server.stopServer();
            serverHarness = new RestServerHarness<>(getRestServerClass());
            serverHarness.setConfigurations(getConfigurations());
            serverHarness.addConfigurationFilter(this);
            serverHarness.init(getServerEnvironment());
            server = serverHarness.getServer();
            server.addLifecycleListener(this);
            serverHarness.startServer();
        }
    }

    protected void createDb(String dbName) throws IOException { notSupported("createDb: must be defined in subclass"); }
    protected boolean dropDb(String dbName) throws IOException { return notSupported("dropDb: must be defined in subclass"); }

    private boolean requireNewServer(RestServer server) {
        if (server == null) return true;
        if (!useTestSpecificDatabase() || !(server.getConfiguration() instanceof HasDatabaseConfiguration)) return false;
        // Is this "leftover" from a previous test?
        final DatabaseConfiguration database = ((HasDatabaseConfiguration) server.getConfiguration()).getDatabase();
        return !database.getDatabaseName().startsWith(getTempDbNamePrefix(database.getUrl()));
    }

    @Override public void beforeStart(RestServer<C> server) {
        if (useTestSpecificDatabase() && server.getConfiguration() instanceof HasDatabaseConfiguration) {
            final DatabaseConfiguration database = ((HasDatabaseConfiguration) server.getConfiguration()).getDatabase();
            String url = database.getUrl();
            int lastSlash = url.lastIndexOf('/');
            if (lastSlash == -1 || lastSlash == url.length() - 1) {
                log.warn("initTestDb: couldn't understand url: " + url + ", leaving as is");
                return;
            }
            final String dbName = getTempDbNamePrefix(url) + "_" + randomAlphanumeric(8);
            try {
                dropDb(dbName);
                createDb(dbName);
            } catch (Exception e) {
                die("beforeStart: error dropping/creating database: " + dbName);
            }
            database.setUrl(url.substring(0, lastSlash) + "/" + dbName);
        }
    }

    private String getTempDbNamePrefix(String url) {
        return truncate(url.substring(url.lastIndexOf('/') + 1), 15) + "_" + truncate(camelCaseToSnakeCase(getClass().getSimpleName()), 35);
    }

    protected Map<String, String> getServerEnvironment() throws Exception { return null; }

    @After public void stopServer () throws Exception {
        if (server != null && !shouldCacheServer()) {
            server.stopServer();
            server = null;
        }
    }

    @Override public void onStop(RestServer<C> server) {
        if (useTestSpecificDatabase()) {
            final C configuration = server.getConfiguration();
            if (configuration instanceof HasDatabaseConfiguration) {
                daemon(new DbDropper(((HasDatabaseConfiguration) configuration).getDatabase().getDatabaseName()));
            }
        }
    }

    @AllArgsConstructor
    private class DbDropper implements Runnable {
        private final String dbName;
        @Override public void run() {
            int sleep = 1000;
            for (int i=0; i<5; i++) {
                Sleep.sleep(sleep);
                try {
                    if (dropDb(dbName)) return;
                    log.warn("error dropping database: " + dbName);
                } catch (IOException e) {
                    log.warn("error dropping database: " + dbName + ": " + e);
                }
                sleep *= 4;
            }
            log.error("giving up trying to drop database: " + dbName);
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
