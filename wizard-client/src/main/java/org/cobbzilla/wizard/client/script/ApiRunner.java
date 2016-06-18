package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.javascript.JsEngine;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ConstraintViolationList;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.system.Sleep.sleep;

@AllArgsConstructor @Slf4j
public class ApiRunner {

    public static final String CTX_JSON = "json";
    public static final String RAND = "@@@";
    public static final String DEFAULT_SESSION_NAME = "default";
    public static final String NEW_SESSION = "new";

    private ApiClientBase api;
    private ApiRunnerListener listener;

    protected final Map<String, Object> ctx = new HashMap<>();

    @Getter(lazy=true, value=AccessLevel.PROTECTED) private final Handlebars handlebars = initHandlebars();
    private Handlebars initHandlebars() {
        final Handlebars hb = new Handlebars(new HandlebarsUtil("api-runner(" + api + ")"));
        hb.registerHelper("sha256", new Helper<Object>() {
            public CharSequence apply(Object src, Options options) {
                if (empty(src)) return "";
                src = handlebars(src.toString(), (Map<String, Object>) options.context.model());
                src = ShaUtil.sha256_hex(src.toString());
                return new Handlebars.SafeString(src.toString());
            }
        });
        return hb;
    }

    protected final Map<String, Class> storeTypes = new HashMap<>();
    protected final Map<String, String> namedSessions = new HashMap<>();

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

        if (script.hasDelay()) sleep(script.getDelayMillis(), "delaying before starting script: "+script);
        script.setStart(now());
        do {
            if (runOnce(script)) return true;
            sleep(Math.min(script.getTimeoutMillis()/10, 1000), "waiting to retry script: "+script);
        } while (!script.isTimedOut());
        if (listener != null) listener.scriptTimedOut(script);
        return false;
    }

    public boolean runOnce(ApiScript script) throws Exception {
        final ApiScriptRequest request = script.getRequest();
        final String method = request.getMethod().toUpperCase();
        ctx.put("now", script.getStart());

        if (request.hasSession()) {
            if (request.getSession().equals(NEW_SESSION)) {
                api.logout();
            } else {
                final String sessionId = namedSessions.get(request.getSession());
                if (sessionId == null) die("Session named " + request.getSession() + " is not defined (" + namedSessions + ")");
                api.setToken(sessionId);
            }
        }

        String uri = handlebars(request.getUri(), ctx);
        if (!uri.startsWith("/")) uri = "/" + uri;

        boolean success = true;
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
                Class<?> storeClass;
                if (response.hasType()) {
                    storeClass = forName(response.getType());
                    storeTypes.put(response.getStore(), storeClass);
                } else {
                    storeClass = storeTypes.get(response.getStore());
                }
                if (storeClass == null) {
                    if (responseEntity.isArray()) {
                        storeClass = Map[].class;
                    } else {
                        storeClass = Map.class;
                    }
                }
                try {
                    responseObject = fromJsonOrDie(responseEntity, storeClass);
                } catch (IllegalStateException e) {
                    log.warn("runOnce: error parsing JSON: "+e);
                    responseObject = responseEntity;
                }

                if (response.hasStore()) ctx.put(response.getStore(), responseObject);

                if (response.hasSession()) {
                    final JsonNode sessionIdNode = JsonUtil.findNode(responseEntity, response.getSession());
                    if (sessionIdNode == null) {
                        if (listener != null) listener.sessionIdNotFound(script, restResponse);
                    } else {
                        final String sessionId = sessionIdNode.textValue();
                        if (empty(sessionId)) die("empty sessionId: "+restResponse);
                        final String sessionName = response.hasSessionName() ? response.getSessionName() : DEFAULT_SESSION_NAME;
                        namedSessions.put(sessionName, sessionId);
                        api.setToken(sessionId);
                    }
                }
            }

            if (response.hasChecks()) {
                final Map<String, Object> localCtx = new HashMap<>();
                localCtx.putAll(ctx);
                localCtx.put(CTX_JSON, responseObject);

                for (ApiScriptResponseCheck check : response.getCheck()) {
                    final String condition = handlebars(check.getCondition(), localCtx);
                    Boolean result = null;
                    long timeout = check.getTimeoutMillis();
                    long checkStart = now();
                    do {
                        try {
                            result = JsEngine.evaluate(condition, scriptName(script, condition), localCtx, Boolean.class);
                            if (result != null && result) break;
                            log.warn("run("+script+"): condition check ("+condition+") returned false");
                        } catch (Exception e) {
                            log.warn("run(" + script + "): condition check ("+condition+") failed: " + e);
                        }
                        sleep(Math.min(timeout/10, 1000), "waiting to retry condition: "+condition);
                    } while (now() - checkStart < timeout);

                    if (result == null || !result) {
                        success = false;
                        if (listener != null) listener.conditionCheckFailed(script, restResponse, check);
                    }
                }
            }

        } else if (restResponse.status != HttpStatusCodes.OK) {
            success = false;
            if (listener != null) listener.unexpectedResponse(script, restResponse);
        }

        if (listener != null) listener.scriptCompleted(script);
        return success;
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
        return HandlebarsUtil.apply(getHandlebars(), value, ctx);
    }

}
