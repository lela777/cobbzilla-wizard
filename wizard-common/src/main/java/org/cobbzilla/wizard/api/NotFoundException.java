package org.cobbzilla.wizard.api;

import org.cobbzilla.wizard.util.RestResponse;

public class NotFoundException extends ApiException {

    public NotFoundException(RestResponse response) {
        super(response);
    }

}
