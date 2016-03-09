package org.cobbzilla.wizard.model;

import javax.persistence.MappedSuperclass;
import java.util.UUID;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@MappedSuperclass
public class StrongIdentifiableBase extends IdentifiableBase {

    @Override public void initUuid() { setUuid(newStrongUuid()); }

    public static String newStrongUuid() { return UUID.randomUUID().toString() + "-" + Long.toHexString(now()); }

    public static boolean isStrongUuid(String maybeUuid) {
        // must be non-empty, at least 40 chars, and cannot contain any letter chars that
        return !empty(maybeUuid)
                && maybeUuid.length() >= 40
                && maybeUuid.replaceAll("[\\WA-Fa-f\\d]+", "").length() == 0;
    }
}
