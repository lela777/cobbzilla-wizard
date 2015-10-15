package org.cobbzilla.wizard.main;

import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.util.main.BaseMainOptions;
import org.cobbzilla.wizard.util.RestResponse;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public abstract class MainBase<OPT extends BaseMainOptions> extends BaseMain<OPT> {

    protected void out (RestResponse response) {
        out(response.isSuccess() && !empty(response.json) ? response.json : response.toString());
    }

}
