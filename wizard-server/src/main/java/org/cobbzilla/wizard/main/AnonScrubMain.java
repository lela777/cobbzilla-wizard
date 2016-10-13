package org.cobbzilla.wizard.main;

import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.wizard.model.anon.AnonScrubber;

public class AnonScrubMain<OPT extends AnonScrubOptions> extends BaseMain<OPT> {

    @Override protected void run() throws Exception {
        final OPT options = getOptions();
        final AnonScrubber scrubber = new AnonScrubber().setTables(options.getScrubs());
        scrubber.anonymize(options.getDatabaseReadConfiguration(),
                           options.getDatabaseWriteConfiguration(),
                           options.isIgnoreUnknown());
    }

}
