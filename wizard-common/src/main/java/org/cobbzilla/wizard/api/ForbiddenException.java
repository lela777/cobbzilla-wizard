package org.cobbzilla.wizard.api;

import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.wizard.util.RestResponse;

public class ForbiddenException extends ApiException {

    public ForbiddenException() { super(new RestResponse(HttpStatusCodes.FORBIDDEN)); }

    public ForbiddenException(RestResponse response) { super(response); }

}
