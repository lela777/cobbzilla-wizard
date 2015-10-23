package org.cobbzilla.wizardtest.resources;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerConfigurationFilter;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.RestServerLifecycleListener;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.junit.After;
import org.junit.Before;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;

@Slf4j
public abstract class AbstractResourceIT<C extends RestServerConfiguration, S extends RestServer<C>>
        extends ApiClientBase implements RestServerLifecycleListener<C>, RestServerConfigurationFilter<C> {

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

    @Override public synchronized String getBaseUri() { return server.getClientUri(); }

    protected C getConfiguration () { return (C) server.getConfiguration(); }

    public boolean shouldCacheServer () { return true; }

    @Before public synchronized void startServer() throws Exception {
        if (serverHarness == null || !shouldCacheServer()) {
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

    protected Map<String, String> getServerEnvironment() throws Exception { return null; }

    @After public void stopServer () throws Exception {
        if (server != null && !shouldCacheServer()) {
            server.stopServer();
            server = null;
        }
    }

    protected Map<String, ConstraintViolationBean> mapViolations(String json) throws Exception {
        Map<String, ConstraintViolationBean> map = new HashMap<>();
        ConstraintViolationBean[] violations = JsonUtil.FULL_MAPPER.readValue(json, ConstraintViolationBean[].class);
        for (ConstraintViolationBean violation : violations) {
            map.put(violation.getMessageTemplate(), violation);
        }
        return map;
    }

    protected void assertExpectedViolations(RestResponse response, String[] violationMessages) throws Exception{
        assertEquals(HttpStatusCodes.UNPROCESSABLE_ENTITY, response.status);
        final Map<String, ConstraintViolationBean> violations = mapViolations(response.json);
        assertEquals(violationMessages.length, violations.size());
        for (String message : violationMessages) {
            assertNotNull(violations.get(message));
        }
    }

    protected ResultSetBean execSql(String sql, Object... args) throws Exception {
        return getConfiguration().execSql(sql, args);
    }
}
