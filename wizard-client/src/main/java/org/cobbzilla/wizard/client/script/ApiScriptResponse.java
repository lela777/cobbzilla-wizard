package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.http.HttpStatusCodes;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;

public class ApiScriptResponse {

    @Getter @Setter private int status = HttpStatusCodes.OK;

    @Getter @Setter private String session;
    public boolean hasSession() { return !empty(session); }

    @Getter @Setter private String sessionName;
    public boolean hasSessionName() { return !empty(sessionName); }

    @Getter @Setter private String store;
    public boolean hasStore() { return !empty(store); }

    @Getter @Setter private String type;
    public boolean hasType() { return !empty(type); }

    @Getter @Setter private ApiScriptResponseCheck[] check;
    public boolean hasChecks() { return !empty(check); }

    @Getter @Setter private boolean raw;

    @Getter @Setter private String delay;
    public boolean hasDelay () { return !empty(delay); }
    public long getDelayMillis () { return parseDuration(delay); }

}
