package org.cobbzilla.wizard.model.anon;

import lombok.Getter;
import lombok.Setter;

public class AnonAllowedEmailDomains {

    @Getter @Setter private static String[] allowedSuffixes = null;

    public static boolean isAllowed(String value) {
        if (allowedSuffixes == null) return false;
        for (String suffix : allowedSuffixes) if (value.endsWith(suffix)) return true;
        return false;
    }

}
