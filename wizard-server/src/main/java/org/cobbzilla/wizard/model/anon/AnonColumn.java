package org.cobbzilla.wizard.model.anon;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.json.JsonUtil;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.regex.Pattern;

import static org.cobbzilla.util.json.JsonUtil.findNode;
import static org.cobbzilla.util.json.JsonUtil.replaceNode;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Accessors(chain = true) @ToString(of="name")
public class AnonColumn {

    @Getter @Setter private String name;
    @Getter @Setter private boolean encrypted = false;
    @Getter @Setter private String value;
    @Getter @Setter private AnonJsonPath[] json;
    @Getter @Setter private String[] skip;
    @Setter private AnonType type;
    public AnonType getType() { return type != null ? type : AnonType.guessType(getName()); }

    public void setParam(PreparedStatement ps,
                         HibernatePBEStringEncryptor decryptor,
                         HibernatePBEStringEncryptor encryptor,
                         int index, String value) throws Exception {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            if (encrypted) value = decryptor.decrypt(value);

            if (!shouldSkip(value)) {
                if (this.value != null) {
                    value = this.value;
                } else {
                    if (json == null) {
                        value = getType().transform(value);
                    } else {
                        value = transformJson(value);
                    }
                }
            }

            if (encrypted && value != null) value = encryptor.encrypt(value);

            if (value == null) {
                ps.setNull(index, Types.VARCHAR);
            } else {
                ps.setString(index, value);
            }
        }
    }

    private String transformJson(String value) throws Exception {
        ObjectNode node = JsonUtil.json(value, ObjectNode.class);
        for (AnonJsonPath jsonPath : json) {
            final JsonNode toReplace = findNode(node, jsonPath.getPath());
            if (toReplace != null) {
                final String val = toReplace.textValue();
                node = replaceNode(node, jsonPath.getPath(), jsonPath.getType().transform(val));
            }
        }
        return toJson(node);
    }

    @Getter(lazy=true) private final Pattern[] skipPatterns = initSkipPatterns();
    private Pattern[] initSkipPatterns() {
        final Pattern[] patterns = new Pattern[skip == null ? 0 : skip.length];
        for (int i=0; i<skip.length; i++) patterns[i] = Pattern.compile(skip[i]);
        return patterns;
    }

    private boolean shouldSkip(String value) {
        if (skip == null || skip.length == 0) return true;
        for (Pattern p : getSkipPatterns()) {
            if (p.matcher(value).find()) return true;
        }
        return false;
    }

}
