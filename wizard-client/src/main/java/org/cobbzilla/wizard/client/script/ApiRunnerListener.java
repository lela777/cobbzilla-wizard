package org.cobbzilla.wizard.client.script;

import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

public interface ApiRunnerListener {

    void beforeCall(ApiScript script, Map<String, Object> ctx);

    void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response);

    void statusCheckFailed(ApiScript script, RestResponse restResponse);
    void conditionCheckFailed(ApiScript script, RestResponse restResponse, ApiScriptResponseCheck check);

    void sessionIdNotFound(ApiScript script, RestResponse restResponse);

    void scriptCompleted(ApiScript script);

    void scriptTimedOut(ApiScript script);

    void unexpectedResponse(ApiScript script, RestResponse restResponse);
}
