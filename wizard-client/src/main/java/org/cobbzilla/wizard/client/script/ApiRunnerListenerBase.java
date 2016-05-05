package org.cobbzilla.wizard.client.script;

import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ApiRunnerListenerBase implements ApiRunnerListener {

    @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {}

    @Override public void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response) {}

    @Override public void statusCheckFailed(int expected, int actual) {
        die("statusCheckFailed: expected "+expected+" but was "+actual);
    }

    @Override public void conditionCheckFailed(ApiScriptResponseCheck check) {
        die("conditionCheckFailed: "+check);
    }

    @Override public void sessionIdNotFound(ApiScriptResponse response, RestResponse restResponse) {
        die("sessionIdNotFound: expected "+response.getSession()+", response was: "+restResponse);
    }

    @Override public void scriptCompleted(ApiScript script) {}

}
