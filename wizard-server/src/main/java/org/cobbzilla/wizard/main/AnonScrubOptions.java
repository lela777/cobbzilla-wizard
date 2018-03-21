package org.cobbzilla.wizard.main;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.anon.AnonTable;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public List<AnonTable> getScrubs() {
        final String json = scrubFile != null ? toStringOrDie(scrubFile) : stream2string(DEFAULT_SCRUB_RESOURCE);
        final AnonTable[] tables = json(json, AnonTable[].class, FULL_MAPPER_ALLOW_COMMENTS);

        final List<String> tableNamesToAnonymize = getTablesList();
        if (empty(tableNamesToAnonymize)) return Arrays.asList(tables);
        final List<AnonTable> tablesToAnonymize = new ArrayList<>();
        for (AnonTable t : tables) {
            if (shouldAnonymizeTable(t.getTable())) tablesToAnonymize.add(t);
        }
        if (tablesToAnonymize.size() != getTablesList().size()) {
            tableNamesToAnonymize.removeIf(t -> tablesToAnonymize.stream().anyMatch(a -> a.getTable().equals(t)));
            return die("getScrubs: tables specified via "+OPT_TABLES+"/"+LONGOPT_TABLES+" do not exist:\n"+StringUtil.toString(tableNamesToAnonymize, "\n"));
        }
        return tablesToAnonymize;
    }

    public static final String USAGE_TABLES = "Only anonymize these tables. Use a comma-separated list with no spaces";
    public static final String OPT_TABLES = "-t";
    public static final String LONGOPT_TABLES= "--tables";
    @Option(name=OPT_TABLES, aliases=LONGOPT_TABLES, usage=USAGE_TABLES)
    @Getter @Setter private String tables = null;

    public boolean shouldAnonymizeTable (String table) { return empty(tables) || getTablesList().contains(table); }

    public List<String> getTablesList() { return empty(tables) ? null : StringUtil.split(tables, ", "); }

}
