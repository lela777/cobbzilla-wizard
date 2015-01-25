package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class MainOptionsBase {

    public static final String USAGE_HELP = "Show help for this command";
    public static final String OPT_HELP = "-h";
    public static final String LONGOPT_HELP= "--help";
    @Option(name=OPT_HELP, aliases=LONGOPT_HELP, usage=USAGE_HELP)
    @Getter @Setter private boolean help;

}
