package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ConstraintViolationList;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

@AllArgsConstructor @Slf4j
public class ApiRunner {

    public static final String CTX_JSON = "json";
    public static final String RAND = "@@@";

    private ApiClientBase api;
    private ApiRunnerListener listener;

    protected final Map<String, Object> ctx = new HashMap<>();
    protected final Handlebars handlebars = new Handlebars(new HandlebarsUtil("api-runner("+api+")"));
    protected final Map<String, Class> storeTypes = new HashMap<>();

    public void reset () {
        ctx.clear();
        storeTypes.clear();
        api.logout();
    }

    public void run(String script) throws Exception {
        run(json(script, ApiScript[].class, FULL_MAPPER_ALLOW_COMMENTS));
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
                final Class<?> storeClass;
                if (response.hasType()) {
                    storeClass = forName(response.getType());
                    storeTypes.put(response.getStore(), storeClass);
                } else {
                    storeClass = storeTypes.get(response.getStore());
                }
                if (storeClass != null) responseObject = fromJsonOrDie(responseEntity, storeClass);

                if (response.hasStore()) ctx.put(response.getStore(), responseObject);

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

        } else if (restResponse.status != HttpStatusCodes.OK) {
            if (listener != null) listener.unexpectedResponse(script, restResponse);
        }

        if (listener != null) listener.scriptCompleted(script);
        return true;
    }

    protected String subst(ApiScriptRequest request) {
        String json = requestEntityJson(request);
        if (json != null) {
            while (json.contains(RAND)) json = json.replaceFirst(RAND, randomAlphanumeric(10));
        }
        return json;
    }

    protected String requestEntityJson(ApiScriptRequest request) {
        return handlebars(request.getJsonEntity(ctx), ctx);
    }

    protected String scriptName(ApiScript script, String name) { return "api-runner(" + script + "):" + name; }

    protected String handlebars(String value, Map<String, Object> ctx) {
        return HandlebarsUtil.apply(handlebars, value, ctx);
    }

}
