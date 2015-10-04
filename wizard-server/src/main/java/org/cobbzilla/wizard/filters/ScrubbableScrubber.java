package org.cobbzilla.wizard.filters;

import javax.ws.rs.ext.Provider;

@Provider
public class ScrubbableScrubber extends ResultScrubber {

    public static final ScrubbableField[] NOTHING_TO_SCRUB = new ScrubbableField[0];

    @Override protected ScrubbableField[] getFieldsToScrub(Object entity) {
        if (entity instanceof Scrubbable) {
            return ((Scrubbable) entity).fieldsToScrub();
        }
        return NOTHING_TO_SCRUB;
    }

}
