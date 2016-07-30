package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.util.RestResponse;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

public class ApiRunnerListenerBase implements ApiRunnerListener {

    @Getter @Setter private ApiRunnerListener beforeHandler;
    @Getter @Setter private ApiRunnerListener afterHandler;

    @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {}

    @Override public void afterCall(ApiScript script, Map<String, Object> ctx, RestResponse response) {}

    @Override public void statusCheckFailed(ApiScript script, RestResponse restResponse) {
        if (script.isTimedOut()) die("statusCheckFailed: expected "+script.getResponse().getStatus()+" but was "+restResponse.status);
    }

    @Override public void conditionCheckFailed(ApiScript script, RestResponse restResponse, ApiScriptResponseCheck check) {
        if (script.isTimedOut()) die("conditionCheckFailed("+script+"):\nfailed condition="+check+"\nserver response="+restResponse);
    }

    @Override public void sessionIdNotFound(ApiScript script, RestResponse restResponse) {
        if (script.isTimedOut()) die("sessionIdNotFound: expected "+script.getResponse().getSession()+", server response="+restResponse);
    }

    @Override public void scriptCompleted(ApiScript script) {}

    @Override public void scriptTimedOut(ApiScript script) { die("scriptTimedOut: script="+script+", timed out"); }

    @Override public void unexpectedResponse(ApiScript script, RestResponse restResponse) {
        if (script.isTimedOut()) die("unexpectedResponse: script="+script+", server response="+restResponse);
    }

    @Override public void handleBefore(String before) throws Exception {
        if (beforeHandler == null) notSupported("handleBefore("+before+")");
        beforeHandler.handleBefore(before);
    }

    @Override public void handleAfter(String after) throws Exception {
        if (afterHandler == null) notSupported("handleAfter("+after+")");
        afterHandler.handleAfter(after);
    }

}
