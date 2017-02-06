package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static org.cobbzilla.util.time.TimeUtil.parseDuration;

@ToString(of="condition")
public class ApiScriptResponseCheck {

    @Getter @Setter private String condition;

    @Getter @Setter private String timeout;
    public long getTimeoutMillis () { return parseDuration(timeout); }

}
