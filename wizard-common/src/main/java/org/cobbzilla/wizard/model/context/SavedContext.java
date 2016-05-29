package org.cobbzilla.wizard.model.context;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;
import static org.cobbzilla.wizard.model.crypto.EncryptedTypes.ENCRYPTED_STRING;
import static org.cobbzilla.wizard.model.crypto.EncryptedTypes.ENC_PAD;

@Embeddable @NoArgsConstructor @Accessors(chain=true)
public class SavedContext {

    public static final int CONTEXT_JSON_MAXLEN = 100_000;

    @Size(max=CONTEXT_JSON_MAXLEN, message="err.contextJson.length")
    @Column(columnDefinition="varchar("+(CONTEXT_JSON_MAXLEN+ENC_PAD)+") NOT NULL")
    @JsonIgnore @Type(type=ENCRYPTED_STRING) @Getter @Setter private String contextJson = "[]";

    public SavedContext(Map<String, Object> context) { setContext(context); }

    @Transient public Map<String, Object> getContext () {
        final ContextEntry[] entries = json(contextJson, ContextEntry[].class);
        final Map<String, Object> map = new LinkedHashMap<>();
        for (ContextEntry entry : entries) {
            map.put(entry.getName(), json(entry.getJson(), forName(entry.getClassName())));
        }
        return map;
    }
    public void setContext(Map<String, Object> map) {
        final List<ContextEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            final Object thing = entry.getValue();
            entries.add(new ContextEntry(entry.getKey(), thing.getClass().getName(), json(thing)));
        }
        contextJson = json(entries);
    }

}
