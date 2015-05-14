package org.cobbzilla.wizard.main;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public abstract class MainBase<OPT extends MainOptionsBase> {

    @Getter private final OPT options = initOptions();
    protected OPT initOptions() { return instantiate((Class<OPT>) getFirstTypeParam(getClass())); }

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
        parser.printUsage(System.err);
        die(e.getMessage());
    }

    protected void out (String message) { System.out.println(message); }
    protected void err (String message) { System.err.println(message); }

    protected void die (String message) {
        log.error(message);
        err(message);
        System.exit(1);
    }

    protected void die (String message, Exception e) {
        log.error(message, e);
        err(message + ": " + e);
        System.exit(1);
    }
}
