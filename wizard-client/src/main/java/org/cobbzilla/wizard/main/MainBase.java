package org.cobbzilla.wizard.main;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

@Slf4j
public abstract class MainBase<OPT extends MainOptionsBase> {

    @Getter private final OPT options = initOptions();
    protected abstract OPT initOptions();

    @Getter(value= AccessLevel.PROTECTED) private final CmdLineParser parser = new CmdLineParser(getOptions());

    protected abstract void run() throws Exception;

    @Getter private String[] args;
    public void setArgs(String[] args) throws CmdLineException {
        this.args = args;
        try {
            parser.parseArgument(args);
            if (options.isHelp()) {
                showHelpAndExit();
            }

        } catch (Exception e) {
            showHelpAndExit(e);
        }
    }

    protected void preRun() {}
    protected void postRun() {}

    protected static void main(Class<? extends MainBase> clazz, String[] args) {
        try {
            final MainBase m = clazz.newInstance();
            m.setArgs(args);
            m.preRun();
            m.run();
            m.postRun();

        } catch (Exception e) {
            log.error("Unexpected error: " + e, e);
        }
    }

    protected void showHelpAndExit() {
        parser.printUsage(System.out);
        System.exit(0);
    }

    protected void showHelpAndExit(String error) {
        showHelpAndExit(new IllegalArgumentException(error));
    }

    protected void showHelpAndExit(Exception e) {
        System.err.println(e.getMessage());
        parser.printUsage(System.err);
        System.exit(1);
    }

}
