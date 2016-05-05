package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.StringTemplateLoader;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ConstraintViolationList;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

@AllArgsConstructor @Slf4j
public class ApiRunner {

    public static final String CTX_JSON = "json";
    public static final String RAND = "@@@";

    private ApiClientBase api;
    private ApiRunnerListener listener;

    private final Map<String, Object> ctx = new HashMap<>();
    private final Handlebars handlebars = new Handlebars(new StringTemplateLoader("api-runner("+api+")"));
    private final Map<String, Class> storeTypes = new HashMap<>();

    public void run(String script) throws Exception {
        run(json(script, ApiScript[].class));
    }

    public void run(ApiScript[] scripts) throws Exception {
        for (ApiScript script : scripts) run(script);
    }

    public boolean run(ApiScript script) throws Exception {

        final ApiScriptRequest request = script.getRequest();
        final String method = request.getMethod().toUpperCase();

        String uri = handlebars(request.getUri(), ctx);
        if (!uri.startsWith("/")) uri = "/" + uri;

        final RestResponse restResponse;

        if (listener != null) listener.beforeCall(script, ctx);
        switch (method) {
            case HttpMethods.GET:
                restResponse = api.doGet(uri);
                break;

            case HttpMethods.PUT:
                restResponse = api.doPut(uri, subst(request));
                break;

            case HttpMethods.POST:
                restResponse = api.doPost(uri, subst(request));
                break;

            case HttpMethods.DELETE:
                restResponse = api.doDelete(uri);
                break;

            default:
                return die("run("+script+"): invalid request method: "+method);
        }
        if (listener != null) listener.afterCall(script, ctx, restResponse);

        if (script.hasResponse()) {
            final ApiScriptResponse response = script.getResponse();

            if (response.getStatus() != restResponse.status) {
                if (listener != null) listener.statusCheckFailed(script, restResponse);
            }

            final JsonNode responseEntity = empty(restResponse.json) ? null : json(restResponse.json, JsonNode.class);
            Object responseObject = responseEntity;

            if (response.getStatus() == HttpStatusCodes.UNPROCESSABLE_ENTITY) {
                responseObject = new ConstraintViolationList(fromJsonOrDie(responseEntity, ConstraintViolationBean[].class));
            } else {
                if (response.hasStore()) {
                    final Class<?> storeClass;
                    if (response.hasStoreType()) {
                        storeClass = forName(response.getStoreType());
                        storeTypes.put(response.getStore(), storeClass);
                    } else {
                        storeClass = storeTypes.get(response.getStore());
                    }
                    if (storeClass != null) responseObject = fromJsonOrDie(responseEntity, storeClass);
                    ctx.put(response.getStore(), responseObject);
                }

                if (response.hasSession()) {
                    final JsonNode sessionIdNode = JsonUtil.findNode(responseEntity, response.getSession());
                    if (sessionIdNode == null) {
                        if (listener != null) listener.sessionIdNotFound(script, restResponse);
                    } else {
                        api.pushToken(sessionIdNode.textValue());
                    }
                }
            }

            if (response.hasChecks()) {
                final Map<String, Object> localCtx = new HashMap<>();
                localCtx.putAll(ctx);
                localCtx.put(CTX_JSON, responseObject);

                for (ApiScriptResponseCheck check : response.getCheck()) {
                    final String condition = check.getCondition();
                    Boolean result = null;
                    try {
                        result = JsEngine.evaluate(condition, scriptName(script, condition), localCtx, Boolean.class);
                    } catch (Exception e) {
                        log.warn("run("+script+"): script execution failed: "+e);
                    }
                    if (result == null || !result) {
                        if (listener != null) listener.conditionCheckFailed(script, restResponse, check);
                    }
                }
            }
        }

        if (listener != null) listener.scriptCompleted(script);
        return true;
    }

    protected String subst(ApiScriptRequest request) {
        String json = request.getJsonEntity(ctx);
        if (json != null) {
            while (json.contains(RAND)) json = json.replaceFirst(RAND, randomAlphanumeric(10));
        }
        return json;
    }

    protected String scriptName(ApiScript script, String name) { return "api-runner(" + script + "):" + name; }

    protected String handlebars(String value, Map<String, Object> ctx) throws IOException {
        @Cleanup final StringWriter writer = new StringWriter(value.length());
        handlebars.compile(value).apply(ctx, writer);
        return writer.toString();
    }

}
