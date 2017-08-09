package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.jknack.handlebars.Handlebars;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.Transformer;
import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.HttpClientContext;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.http.HttpMethods;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.javascript.StandardJsEngine;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizard.util.TestNames;
import org.cobbzilla.wizard.validation.ConstraintViolationBean;
import org.cobbzilla.wizard.validation.ValidationErrors;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.*;
import static org.cobbzilla.util.string.StringUtil.replaceWithRandom;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.wizard.client.script.ApiScript.PARAM_REQUIRED;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @Slf4j
public class ApiRunner {

    public static final String CTX_JSON = "json";
    public static final String CTX_RESPONSE = "response";
    public static final String RAND = "@@@";
    public static final String DEFAULT_SESSION_NAME = "default";
    public static final String NEW_SESSION = "new";

    private StandardJsEngine js = new StandardJsEngine();

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") // intended for use in debugging
    @Getter private static Map<String, ApiScript> currentScripts = new HashMap<>();

    public ApiRunner(ApiClientBase api, ApiRunnerListener listener) {
        this.api = api;
        this.listener = listener;
    }

    public ApiRunner(ApiRunner other, HttpClient httpClient) {
        copy(this, other);
        this.api = other.api.copy();
        this.api.setHttpClient(httpClient);
        this.api.setHttpContext(HttpClientContext.create());
        this.listener = copy(other.listener);
        this.ctx.putAll(other.ctx);
    }

    public void setScriptForThread(ApiScript script) {
        if (script.hasComment()) log.info(script.getComment());
        currentScripts.put(Thread.currentThread().getName(), script);
    }

    // be careful - if you have multiple ApiRunners in the same classloader, these methods will not be useful
    // intended for convenience in the common case of a single ApiRunner
    public static ApiScript script() { return currentScripts.isEmpty() ? null : currentScripts.values().iterator().next(); }
    public static String comment() { return currentScripts.isEmpty() ? null : currentScripts.values().iterator().next().getComment(); }

    private ApiClientBase api;
    private ApiRunnerListener listener;
    @Getter @Setter private ApiScriptIncludeHandler includeHandler;

    protected final Map<String, Object> ctx = new ConcurrentHashMap<>();
    public Map<String, Object> getContext () { return ctx; }

    @Getter(lazy=true) private final Handlebars handlebars = initHandlebars();
    protected Handlebars initHandlebars() {
        final Handlebars hb = new Handlebars(new HandlebarsUtil("api-runner(" + api + ")"));
        HandlebarsUtil.registerUtilityHelpers(hb);
        HandlebarsUtil.registerCurrencyHelpers(hb);
        HandlebarsUtil.registerDateHelpers(hb);
        return hb;
    }

    protected final Map<String, Class> storeTypes = new HashMap<>();
    protected final Map<String, String> namedSessions = new HashMap<>();
    public void addNamedSession (String name, String token) { namedSessions.put(name, token); }

    public void reset () {
        ctx.clear();
        storeTypes.clear();
        api.logout();
    }

    public ApiScript[] include (ApiScript script) {
        if (includeHandler != null) {
            return jsonWithComments(handlebars(includeHandler.include(script.getInclude()),
                    transformStrings(script.getParams(), new Transformer() {
                        @Override public Object transform(Object o) { return replaceRand(o); }
                    }),
                    script.getParamStartDelim(), script.getParamEndDelim()), ApiScript[].class);
        }
        return notSupported("include("+script.getInclude()+"): no includeHandler set");
    }

    public String replaceRand(Object o) { return replaceWithRandom(o.toString(), RAND, 10); }

    public void run(String script) throws Exception {
        run(jsonWithComments(script, ApiScript[].class));
    }

    public boolean run(ApiScript[] scripts) throws Exception {
        boolean allSucceeded = true;
        for (ApiScript script : scripts) if (!run(script)) allSucceeded = false;
        return allSucceeded;
    }

    public boolean run(ApiScript script) throws Exception {
        if (script.hasInclude()) {
            if (script.isIncludeDefaults()) return true; // skip this block. used in validation before running included script
            final String logPrefix = (script.hasComment() ? script.getComment()+"\n" : "") + ">>> ";
            log.info(logPrefix+"including script: '"+script.getInclude()+"'"+(script.hasParams()?" {"+ StringUtil.toString(NameAndValue.map2list(script.getParams()), ", ")+"}":""));
            ApiScript[] include = include(script);
            boolean paramsChanged = false;
            if (include.length > 0 && include[0].isIncludeDefaults()) {
                final ApiScript defaults = include[0];
                if (empty(defaults.getParams())) {
                    log.warn(logPrefix+"no default parameters set");
                } else {
                    for (Map.Entry<String, Object> param : defaults.getParams().entrySet()) {
                        final String pName = param.getKey();
                        final Object pValue = param.getValue();
                        if (empty(pName)) return die(logPrefix+"empty default param name");
                        if (pValue != null && (!script.hasParams() || !script.getParams().containsKey(pName))) {
                            if ((pValue instanceof String) && pValue.equals(PARAM_REQUIRED)) {
                                return die(logPrefix+"required parameter is undefined: "+pName);
                            }
                            if ((pValue instanceof Boolean) && !((Boolean) pValue)) {
                                continue; // boolean values already default to false, no need to change script
                            }
                            log.info(logPrefix+"parameter '"+pName+"' undefined, using default value ("+pValue+")");
                            script.setParam(pName, pValue);
                            paramsChanged = true;
                        }
                    }
                }
            }
            if (paramsChanged) include = include(script); // re-include because params have changed
            final boolean ok = run(include);
            log.info(">>> included script completed: '"+script.getInclude()+"'"+(script.hasParams()?" {"+ StringUtil.toString(NameAndValue.map2list(script.getParams()), ", ")+"}":"")+", ok="+ok);
            return ok;

        } else {
            setScriptForThread(script);
            if (script.hasDelay()) sleep(script.getDelayMillis(), "delaying before starting script: " + script);
            if (listener != null) listener.beforeScript(script.getBefore(), ctx);
            try {
                script.setStart(now());
                do {
                    if (runOnce(script)) return true;
                    sleep(Math.min(script.getTimeoutMillis() / 10, 1000), "waiting to retry script: " + script);
                } while (!script.isTimedOut());
                if (listener != null) listener.scriptTimedOut(script);
                return false;

            } catch (Exception e) {
                log.warn("run(" + script + "): " + e, e);
                throw e;

            } finally {
                if (listener != null) listener.afterScript(script.getAfter(), ctx);
            }
        }
    }

    public boolean runOnce(ApiScript script) throws Exception {

        if (script.hasNested()) return runInner(script);

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

        if (request.hasHeaders()) {
            api.setHeaders(request.getHeaders());
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
                api.removeHeaders();
                break;

            case HttpMethods.POST:
                restResponse = api.doPost(uri, subst(request));
                api.removeHeaders();
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

            final JsonNode responseEntity = empty(restResponse.json) || response.isRaw() ? null : json(restResponse.json, JsonNode.class);
            Object responseObject = responseEntity;

            if (response.getStatus() == HttpStatusCodes.UNPROCESSABLE_ENTITY) {
                responseObject = new ValidationErrors(Arrays.asList(fromJsonOrDie(responseEntity, ConstraintViolationBean[].class)));
            } else {
                Class<?> storeClass = null;
                if (response.hasType()) {
                    storeClass = forName(response.getType());
                    if (response.hasStore()) storeTypes.put(response.getStore(), storeClass);

                } else if (response.hasStore()) {
                    storeClass = storeTypes.get(response.getStore());

                }
                if (storeClass == null && restResponse.hasHeader(api.getEntityTypeHeaderName())) {
                    storeClass = forName(restResponse.header(api.getEntityTypeHeaderName()));
                    if (response.hasStore()) storeTypes.put(response.getStore(), storeClass);
                }
                if (!response.isRaw() && responseEntity != null) {
                    if (storeClass == null) {
                        if (responseEntity.isArray()) {
                            storeClass = Map[].class;
                        } else if (responseEntity.isObject()) {
                            storeClass = Map.class;
                        } else if (responseEntity.isTextual()) {
                            storeClass = String.class;
                        } else if (responseEntity.isIntegralNumber()) {
                            storeClass = Long.class;
                        } else if (responseEntity.isDouble()) {
                            storeClass = Double.class;
                        } else {
                            storeClass = JsonNode.class; // punt
                        }
                    }
                    try {
                        responseObject = fromJsonOrDie(responseEntity, storeClass);
                    } catch (IllegalStateException e) {
                        log.warn("runOnce: error parsing JSON: " + e);
                        responseObject = responseEntity;
                    }

                    if (response.hasStore()) ctx.put(response.getStore(), responseObject);

                    if (response.hasSession()) {
                        final JsonNode sessionIdNode = JsonUtil.findNode(responseEntity, response.getSession());
                        if (sessionIdNode == null) {
                            if (listener != null) listener.sessionIdNotFound(script, restResponse);
                        } else {
                            final String sessionId = sessionIdNode.textValue();
                            if (empty(sessionId)) die("runOnce: empty sessionId: "+restResponse);
                            final String sessionName = response.hasSessionName() ? response.getSessionName() : DEFAULT_SESSION_NAME;
                            namedSessions.put(sessionName, sessionId);
                            api.setToken(sessionId);
                        }
                    }
                }
            }

            ctx.put(CTX_RESPONSE, restResponse);

            if (response.hasChecks()) {
                if (response.hasDelay()) sleep(response.getDelayMillis(), "runOnce: delaying "+response.getDelay()+" before checking response conditions");

                final Map<String, Object> localCtx = new HashMap<>();
                localCtx.putAll(ctx);
                localCtx.put(CTX_JSON, responseObject);

                for (ApiScriptResponseCheck check : response.getCheck()) {
                    if (listener != null && listener.skipCheck(script, check)) continue;
                    final String condition = handlebars(check.getCondition(), localCtx);
                    Boolean result = null;
                    long timeout = check.getTimeoutMillis();
                    long checkStart = now();
                    do {
                        try {
                            result = js.evaluateBoolean(condition, localCtx);
                            if (result) break;
                            if (script.isTimedOut()) {
                                log.warn("runOnce("+script+"): condition check ("+condition+") returned false");
                            } else {
                                log.debug("runOnce("+script+"): condition check ("+condition+") returned false");
                            }
                        } catch (Exception e) {
                            if (script.isTimedOut()) {
                                log.warn("runOnce(" + script + "): condition check (" + condition + ") failed: " + e);
                            } else {
                                log.debug("runOnce(" + script + "): condition check (" + condition + ") failed: " + e);
                            }
                        }
                        sleep(Math.min(timeout/10, 1000), "waiting to retry condition: "+condition);
                    } while (now() - checkStart < timeout);

                    if (result == null || !result) {
                        success = false;
                        final String msg = result == null ? "Exception in execution" : "Failed condition";
                        if (listener != null) listener.conditionCheckFailed(msg, script, restResponse, check, localCtx);
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

    private boolean runInner(ApiScript script) throws Exception {
        final ApiInnerScript inner = script.getNested();
        inner.setParent(script);
        final List<ApiScript> scripts = inner.getAllScripts(js, getHandlebars(), ctx);
        for (ApiScript s : scripts) {
            if (!run(s)) return false;
        }
        return true;
    }

    protected String subst(ApiScriptRequest request) {
        String json = requestEntityJson(request);
        if (json != null) json = replaceRand(json);
        json = TestNames.replaceTestNames(json);
        if (json != null && (json.startsWith("\"") && json.endsWith("\""))) {
            json = json.substring(1, json.length()-1);
        }
        return json;
    }

    protected String requestEntityJson(ApiScriptRequest request) {
        final String json = request.getJsonEntity(ctx);
        return request.isHandlebarsEnabled() ? handlebars(json, ctx) : json;
    }

    protected String scriptName(ApiScript script, String name) { return "api-runner(" + script + "):" + name; }

    protected String handlebars(String value, Map<String, Object> ctx) {
        return HandlebarsUtil.apply(getHandlebars(), value, ctx);
    }

    protected String handlebars(String value, Map<String, Object> ctx, char altStart, char altEnd) {
        return HandlebarsUtil.apply(getHandlebars(), value, ctx, altStart, altEnd);
    }

}
