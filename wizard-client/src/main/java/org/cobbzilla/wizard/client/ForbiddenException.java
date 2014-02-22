package org.cobbzilla.wizard.client;

import org.cobbzilla.wizard.util.RestResponse;

public class ForbiddenException extends ApiException {

    public ForbiddenException(RestResponse response) {
        super(response);
    }

}
