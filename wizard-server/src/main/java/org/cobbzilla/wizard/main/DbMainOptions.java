package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.system.CommandShell.loadShellExportsOrDie;
import static org.cobbzilla.wizard.server.RestServerBase.getStreamConfigurationSources;

public abstract class DbMainOptions extends BaseMainOptions {

    public static final String USAGE_ENV_FILE = "Environment file. Default is ~/.qbis.env";
    public static final String OPT_ENV_FILE = "-e";
    public static final String LONGOPT_ENV_FILE= "--env-file";
    @Option(name=OPT_ENV_FILE, aliases=LONGOPT_ENV_FILE, usage=USAGE_ENV_FILE)
    @Getter @Setter private File envFile = new File(System.getProperty("user.home"), ".qbis.env");

    public abstract String getServerClass();
    public abstract String[] getConfigPaths();

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDbConfig() {
        final Class<S> serverClass = (Class<S>) forName(getServerClass());
        final RestServerHarness<C, S> harness = new RestServerHarness<>(serverClass);

        harness.addConfigurations(getStreamConfigurationSources(serverClass, getConfigPaths()));
        harness.init(loadShellExportsOrDie(getEnvFile()));

        return (HasDatabaseConfiguration) harness.getConfiguration();
    }

    public DatabaseConfiguration getDatabase() { return getDbConfig().getDatabase(); }

}
