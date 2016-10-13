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
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.util.system.CommandShell.loadShellExportsOrDie;
import static org.cobbzilla.wizard.server.RestServerBase.getStreamConfigurationSources;

public abstract class DbMainOptions extends BaseMainOptions {

    public static final File DEFAULT_ENV_FILE = new File(System.getProperty("user.home"), ".db.env");

    public abstract String getServerClass();
    public abstract String[] getConfigPaths();
    public abstract String getDefaultCryptEnvVar();

    public static final String USAGE_ENV_FILE = "Environment file. Default is ~/db.env";
    public static final String OPT_ENV_FILE = "-e";
    public static final String LONGOPT_ENV_FILE= "--env-file";
    @Option(name=OPT_ENV_FILE, aliases=LONGOPT_ENV_FILE, usage=USAGE_ENV_FILE)
    @Getter @Setter private File envFile = DEFAULT_ENV_FILE;

    public static final String USAGE_CRYPT_ENV_VAR = "Name of env var containing encryption/decryption key within env file";
    public static final String OPT_CRYPT_ENV_VAR = "-C";
    public static final String LONGOPT_CRYPT_ENV_VAR= "--crypt-var";
    @Option(name=OPT_CRYPT_ENV_VAR, aliases=LONGOPT_CRYPT_ENV_VAR, usage=USAGE_CRYPT_ENV_VAR)
    @Getter @Setter private String cryptKeyEnvVar = getDefaultCryptEnvVar();

    public static final String USAGE_DECRYPT_ENV_VAR = "Name of env var containing decryption key to use for column-level encryption when reading.";
    public static final String OPT_DECRYPT_ENV_VAR = "-D";
    public static final String LONGOPT_DECRYPT_ENV_VAR= "--decrypt-with";
    @Option(name=OPT_DECRYPT_ENV_VAR, aliases=LONGOPT_DECRYPT_ENV_VAR, usage=USAGE_DECRYPT_ENV_VAR)
    @Getter @Setter private String decryptKeyEnvVar = getDefaultCryptEnvVar();

    public static final String USAGE_ENCRYPT_ENV_VAR = "Name of env var containing encryption key to use for column-level encryption when writing.";
    public static final String OPT_ENCRYPT_ENV_VAR = "-E";
    public static final String LONGOPT_ENCRYPT_ENV_VAR= "--encrypt-with";
    @Option(name=OPT_ENCRYPT_ENV_VAR, aliases=LONGOPT_ENCRYPT_ENV_VAR, usage=USAGE_ENCRYPT_ENV_VAR)
    @Getter @Setter private String encryptKeyEnvVar = null;

    public static final String USAGE_IGNORE_UNKNOWN_COLS = "If false, refuse to run if columns in scrub file do not exist in database. Default is true.";
    public static final String OPT_IGNORE_UNKNOWN_COLS = "-I";
    public static final String LONGOPT_IGNORE_UNKNOWN_COLS= "--ignore-unknown-columns";
    @Option(name=OPT_IGNORE_UNKNOWN_COLS, aliases=LONGOPT_IGNORE_UNKNOWN_COLS, usage=USAGE_IGNORE_UNKNOWN_COLS)
    @Getter @Setter private boolean ignoreUnknownColumns = true;

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDatabaseConfiguration() {
        final String[] paths = getConfigPaths();
        final String decryptKeyVar = getDecryptKeyEnvVar();
        final String cryptKeyVar = getCryptKeyEnvVar();

        // if neither decrypt/crypt env vars are specified, or if they are the same, then don't override key
        final String key = decryptKeyVar == null && cryptKeyVar == null || (decryptKeyVar != null && cryptKeyVar != null && decryptKeyVar.equals(cryptKeyVar)) ? null : decryptKeyVar;
        return getDatabaseConfiguration(paths, key);
    }

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDatabaseConfiguration(String[] paths, String envVar) {
        final String key;
        if (envVar != null) {
            key = System.getenv(envVar);
            if (empty(key)) die("env var not found: " + envVar);
        } else {
            key = null;
        }

        final Class<S> serverClass = (Class<S>) forName(getServerClass());
        final RestServerHarness<C, S> harness = new RestServerHarness<>(serverClass);

        harness.addConfigurations(getStreamConfigurationSources(serverClass, paths));
        final Map<String, String> env = loadShellExportsOrDie(getEnvFile());
        if (key != null) env.put(getDefaultCryptEnvVar(), key);
        harness.init(env);

        return (HasDatabaseConfiguration) harness.getConfiguration();
    }

    public DatabaseConfiguration getDatabase() { return getDatabaseConfiguration().getDatabase(); }

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDatabaseReadConfiguration() {
        return getDatabaseConfiguration(getConfigPaths(), getDecryptKeyEnvVar());
    }

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDatabaseWriteConfiguration() {
        return getDatabaseConfiguration(getConfigPaths(), getEncryptKeyEnvVar());
    }

}
