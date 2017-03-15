package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jknack.handlebars.Handlebars;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.Identifiable;

import java.io.File;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.getNodeAsJava;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.wizard.model.entityconfig.ModelSetup.id;

@Slf4j
public class StandardModelVerifyLog implements ModelVerifyLog {

    @Getter @Setter private File verifyLogFile;
    @Getter @Setter private Handlebars handlebars;
    @Getter private final List<ModelDiffEntry> diffs = new ArrayList<>();

    public boolean enableTextDiffs () { return false; }

    public StandardModelVerifyLog (File f, Handlebars h) {
        verifyLogFile = f;
        handlebars = h;
    }

    @Override public void startLog() {}

    @Override public void endLog() {
        final Map<String, Object> ctx = new HashMap<>();
        ctx.put("diffs", diffs);
        FileUtil.toFileOrDie(verifyLogFile, HandlebarsUtil.apply(handlebars, loadResourceAsStringOrDie(ModelVerifyLog.HTML_TEMPLATE), ctx));
    }

    @Override public void logDifference(EntityConfig entityConfig, Identifiable existing, Identifiable entity) {
        if (!(entity instanceof ModelEntity)) {
            die("logDifference: not a ModelEntity: " + id(entity) + " (is a " + entity.getClass().getName() + ")");
        }
        final ModelEntity request = (ModelEntity) entity;
        final ObjectNode requestNode = request.jsonNode();

        final String existingJson = toBasicJson(existing);
        final String requestJson = toBasicJson(request);

        final String entityId = getEntityId(entityConfig, existing);
        final ModelDiffEntry diffEntry = new ModelDiffEntry(entityId);
        if (!existingJson.equals(requestJson)) {
            final List<String> deltas = new ArrayList<>();
            calculateDiff(requestNode, existing, deltas, "");
            if (empty(deltas) && enableTextDiffs()) {
                diffEntry.setJsonDiff(StringUtil.diff(existingJson, requestJson, null));
            } else {
                diffEntry.setDeltas(deltas);
            }
        }
        if (!diffEntry.isEmpty()) {
            synchronized (diffs) {
                diffs.add(diffEntry);
            }
        }
    }

    private String toBasicJson(Identifiable thing) {
        for (String f : getExcludedFields()) {
            try {
                ReflectionUtil.set(thing, f, null);
            } catch (Exception ignored) {}
        }
        return json(thing instanceof ModelEntity ? ((ModelEntity) thing).getEntity() : thing);
    }

    public String getEntityId(EntityConfig entityConfig, Identifiable existing) {
        return entityConfig.getClassName() + "/" + id(existing);
    }

    @Override public void logCreation(EntityConfig entityConfig, Identifiable entity) {
        diffs.add(new ModelDiffEntry(getEntityId(entityConfig, entity)).setCreateEntity(entity));
    }

    private void calculateDiff(ObjectNode requestNode, Object existing, List<String> deltas, String path) {
        for (Iterator<String> iter = requestNode.fieldNames(); iter.hasNext(); ) {
            final String fieldName = iter.next();
            if (getExcludedFields().contains(fieldName)) continue; // skip children/entity fields
            final Object requestValue = JsonUtil.getNodeAsJava(requestNode.get(fieldName), fieldName);
            final Object existingValue;
            try {
                if (existing instanceof ObjectNode) {
                    existingValue = getNodeAsJava(((ObjectNode) existing).get(fieldName), fieldName);
                } else {
                    existingValue = ReflectionUtil.get(existing, fieldName);
                }
            } catch (Exception e) {
                log.warn("calculateDiff: error fetching " + fieldName + ": " + e);
                continue;
            }
            if (empty(requestValue)) {
                if (empty(existingValue)) continue; // both are nothing
                deltas.add(new StringBuilder().append(fieldName).append(": ").append(existingValue).append(" => [absent]").toString());

            } else if (empty(existingValue)) {
                deltas.add(new StringBuilder().append(fieldName).append(": [absent] => ").append(requestValue).toString());

            } else if (requestValue instanceof ObjectNode) {
                calculateDiff((ObjectNode) requestValue, existingValue, deltas, (empty(path) ? fieldName : path + "." + fieldName));

            } else if (!existingValue.equals(requestValue)) {
                deltas.add(new StringBuilder().append(fieldName).append(": ").append(existingValue).append(" => ").append(requestValue).toString());

            } else {
                // nothing changed
                continue;
            }
        }
    }

    protected static final Set<String> EXCLUDED_FIELDS = new HashSet<>(Arrays.asList("children", "entity"));
    protected Set<String> getExcludedFields() { return EXCLUDED_FIELDS; }

}
