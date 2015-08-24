package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.lang.reflect.Field;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class MainOptionsBase {

    public static final String USAGE_HELP = "Show help for this command";
    public static final String OPT_HELP = "-h";
    public static final String LONGOPT_HELP= "--help";
    @Option(name=OPT_HELP, aliases=LONGOPT_HELP, usage=USAGE_HELP)
    @Getter @Setter private boolean help;

    public void out(String s) { System.out.println(s); }
    public void err(String s) { System.err.println(s); }

    public void required(String field) {
        try {
            final Field optField = getClass().getField("OPT_"+field);
            final Field longOptField = getClass().getField("LONGOPT_"+field);
            err("Missing option: "+optField.get(null)+"/"+longOptField.get(null));
        } catch (Exception e) {
            die("No such field: "+field+": "+e, e);
        }
    }

    public void requiredAndDie(String field) {
        try {
            final Field optField = getClass().getField("OPT_"+field);
            final Field longOptField = getClass().getField("LONGOPT_"+field);
            die("Missing option: "+optField.get(null)+"/"+longOptField.get(null));
        } catch (Exception e) {
            die("No such field: "+field+": "+e, e);
        }
    }
}
