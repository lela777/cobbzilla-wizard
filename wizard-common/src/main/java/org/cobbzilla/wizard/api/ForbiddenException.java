package org.cobbzilla.wizard.api;

import org.cobbzilla.wizard.util.RestResponse;

public class ForbiddenException extends ApiException {

    public ForbiddenException(RestResponse response) {
        super(response);
    }

}
