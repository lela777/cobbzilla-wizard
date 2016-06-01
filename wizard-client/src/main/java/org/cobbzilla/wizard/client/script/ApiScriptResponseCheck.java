package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@ToString(of="condition")
public class ApiScriptResponseCheck {

    @Getter @Setter private String condition;
    @Getter @Setter private String timeout;

    public long getTimeoutMillis () {
        if (empty(timeout)) return 10;
        char lastChar = timeout.charAt(timeout.length()-1);
        if (Character.isLetter(lastChar)) {
            long val = Long.parseLong(timeout.substring(0, timeout.length() - 1));
            switch (lastChar) {
                case 's': return TimeUnit.SECONDS.toMillis(val);
                case 'm': return TimeUnit.MINUTES.toMillis(val);
                case 'h': return TimeUnit.HOURS.toMillis(val);
                case 'd': return TimeUnit.DAYS.toMillis(val);
                default: die("getTimeoutMillis: invalid timeout: "+timeout);
            }
        }
        return Long.parseLong(timeout);
    }

}
