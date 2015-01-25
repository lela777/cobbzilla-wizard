package org.cobbzilla.wizard.main;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public abstract class MainApiBase<OPT extends MainApiOptionsBase> extends MainBase<OPT> {

    @Getter(value=AccessLevel.PROTECTED, lazy=true) private final ApiClientBase apiClient = initApiClient();
    private ApiClientBase initApiClient() {
        return new ApiClientBase(getOptions().getApiBase()) {
            @Override protected String getTokenHeader() { return getApiHeaderTokenName(); }
        };
    }

    @Override protected void preRun() {
        if (getOptions().requireAccount()) login();
    }

    /** @return the Java object to POST as JSON for the login */
    protected abstract Object buildLoginRequest(OPT options);

    /** @return the name of the HTTP header that will hold the session id on future requests */
    protected abstract String getApiHeaderTokenName();

    /** @return the URI to POST the login request to */
    protected abstract String getLoginUri();

    protected abstract String getSessionId(RestResponse response) throws Exception;

    protected void login () {
        final OPT options = getOptions();
        log.info("logging in "+ options.getAccount()+" ...");
        try {
            final Object loginRequest = buildLoginRequest(options);
            final ApiClientBase api = getApiClient();
            final RestResponse response = api.post(getLoginUri(), toJson(loginRequest));
            api.pushToken(getSessionId(response));

        } catch (Exception e) {
            throw new IllegalStateException("Error logging in: "+e, e);
        }
    }

}
