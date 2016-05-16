package org.cobbzilla.wizard.client.script;

import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ApiRunnerListenerBase implements ApiRunnerListener {

    @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {}

    @Override public void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response) {}

    @Override public void statusCheckFailed(ApiScript script, RestResponse restResponse) {
        die("statusCheckFailed: expected "+script.getResponse().getStatus()+" but was "+restResponse.status);
    }

    @Override public void conditionCheckFailed(ApiScript script, RestResponse restResponse, ApiScriptResponseCheck check) {
        die("conditionCheckFailed("+script+"): "+check+", server response="+restResponse);
    }

    @Override public void sessionIdNotFound(ApiScript script, RestResponse restResponse) {
        die("sessionIdNotFound: expected "+script.getResponse().getSession()+", server response="+restResponse);
    }

    @Override public void scriptCompleted(ApiScript script) {}

    @Override public void unexpectedResponse(ApiScript script, RestResponse restResponse) {
        die("unexpectedResponse: script="+script+", server response="+restResponse);
    }

}
