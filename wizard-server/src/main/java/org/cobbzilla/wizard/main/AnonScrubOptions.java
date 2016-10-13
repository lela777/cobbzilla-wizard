package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.anon.AnonTable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.toStringOrDie;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.FULL_MAPPER_ALLOW_COMMENTS;
import static org.cobbzilla.util.json.JsonUtil.json;

public abstract class AnonScrubOptions extends DbMainOptions {

    public static final String DEFAULT_SCRUB_RESOURCE = "db/anonymize.json";

    public static final String USAGE_SCRUB_FILE = "JSON file containing an array of AnonTable objects, determines what is scrubbed. Default classpath resource "+DEFAULT_SCRUB_RESOURCE;
    public static final String OPT_SCRUB_FILE = "-s";
    public static final String LONGOPT_SCRUB_FILE= "--scrub-file";
    @Option(name=OPT_SCRUB_FILE, aliases=LONGOPT_SCRUB_FILE, usage=USAGE_SCRUB_FILE)
    @Getter @Setter private File scrubFile = null;

    public AnonTable[] getScrubs() {
        final String json = scrubFile != null ? toStringOrDie(scrubFile) : stream2string(DEFAULT_SCRUB_RESOURCE);
        return json(json, AnonTable[].class, FULL_MAPPER_ALLOW_COMMENTS);
    }

    public static final String USAGE_ENCRYPT_ENV_VAR = "Name of env var containing encryption key to use for column-level encryption when writing.";
    public static final String OPT_ENCRYPT_ENV_VAR = "-E";
    public static final String LONGOPT_ENCRYPT_ENV_VAR= "--encrypt-with";
    @Option(name=OPT_ENCRYPT_ENV_VAR, aliases=LONGOPT_ENCRYPT_ENV_VAR, usage=USAGE_ENCRYPT_ENV_VAR, required=true)
    @Getter @Setter private String encryptKeyEnvVar = null;

    public <C extends RestServerConfiguration, S extends RestServer<C>> HasDatabaseConfiguration getDatabaseWriteConfiguration() {
        final String encryptionKey = System.getenv(getEncryptKeyEnvVar());
        if (empty(encryptionKey)) die("env var not found: "+getEncryptKeyEnvVar());

        final HasDatabaseConfiguration config = getDatabaseConfiguration();
        config.getDatabase().setEncryptionKey(encryptionKey);
        return config;
    }

}
