package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.string.StringUtil.parseDuration;

@ToString
public class ApiScript {

    @Getter @Setter private String comment;

    @Getter @Setter private String delay;
    public boolean hasDelay () { return !empty(delay); }
    public long getDelayMillis () { return parseDuration(delay); }

    @Getter @Setter private String timeout;
    public long getTimeoutMillis () { return parseDuration(timeout); }

    @Getter @Setter private ApiScriptRequest request;
    @Getter @Setter private ApiScriptResponse response;
    public boolean hasResponse () { return response != null; }

    @Getter @Setter private long start = now();
    public long getAge () { return now() - start; }

    public boolean isTimedOut() { return getAge() > getTimeoutMillis(); }

}
