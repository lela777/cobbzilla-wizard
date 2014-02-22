package org.cobbzilla.wizard.client;

import org.cobbzilla.wizard.util.RestResponse;

public class NotFoundException extends ApiException {

    public NotFoundException(RestResponse response) {
        super(response);
    }

}
