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

import static org.cobbzilla.util.json.JsonUtil.findNode;
import static org.cobbzilla.util.json.JsonUtil.replaceNode;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Accessors(chain = true) @ToString(of="name")
public class AnonColumn {

    @Getter @Setter private String name;
    @Getter @Setter private boolean encrypted = false;
    @Getter @Setter private String value;
    @Getter @Setter private AnonJsonPath[] json;
    @Setter private AnonType type;
    public AnonType getType() { return type != null ? type : AnonType.guessType(getName()); }

    public void setParam(PreparedStatement ps, HibernatePBEStringEncryptor encryptor, int index, String value) throws Exception {
        if (value == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            if (encrypted) value = encryptor.decrypt(value);

            if (this.value != null) {
                value = this.value;
            } else {
                if (json == null) {
                    value = getType().transform(value);
                } else {
                    value = transformJson(value);
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

}
