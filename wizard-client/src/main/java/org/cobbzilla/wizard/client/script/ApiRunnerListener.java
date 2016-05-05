package org.cobbzilla.wizard.client.script;

import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

public interface ApiRunnerListener {

    void beforeCall(ApiScript script, Map<String, Object> ctx);

    void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response);

    void statusCheckFailed(int expected, int actual);
    void conditionCheckFailed(ApiScriptResponseCheck check);

    void sessionIdNotFound(ApiScriptResponse response, RestResponse restResponse);

    void scriptCompleted(ApiScript script);
}
