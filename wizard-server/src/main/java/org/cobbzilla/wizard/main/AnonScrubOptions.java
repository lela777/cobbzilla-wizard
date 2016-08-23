package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.anon.AnonTable;
import org.kohsuke.args4j.Option;

import java.io.File;

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

}
