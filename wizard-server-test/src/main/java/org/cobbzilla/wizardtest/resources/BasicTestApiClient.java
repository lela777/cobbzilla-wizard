package org.cobbzilla.wizardtest.resources;

import org.cobbzilla.wizard.client.ApiClientBase;

public class BasicTestApiClient extends ApiClientBase {

    @Override public synchronized String getBaseUri() { return AbstractResourceIT.server.getClientUri(); }

}
