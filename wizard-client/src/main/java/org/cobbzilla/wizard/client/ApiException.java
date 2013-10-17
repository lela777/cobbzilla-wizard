package org.cobbzilla.wizard.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.wizard.util.RestResponse;

@AllArgsConstructor
public class ApiException extends RuntimeException {

    @Getter private HttpRequestBean request;
    @Getter private RestResponse response;

    public ApiException (RestResponse response) {
        super(response.status+": "+response.json);
        this.response = response;
    }

}
