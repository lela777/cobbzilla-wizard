package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;

@Accessors(chain=true)
public abstract class LdapEntity extends UniquelyNamedEntity {

    @Getter @Setter protected String dn;
    @Transient @JsonIgnore @Getter @Setter protected LdapContext ldapContext;

    // convenience method
    protected LdapContext ldap() { return getLdapContext(); }

    @Override public String getUuid () { return getDn(); }
    @Override public void setUuid (String dn) { /*noop*/  }

    public String getName () { return getDnValue(getDn()); }

    public UniquelyNamedEntity setName (String name) {
        return setDn(getIdField() + "=" + name + "," + getLdapContext().getUser_dn());
    }

    public String getIdField() { return "dn"; }
    public abstract String getParentDn();

    protected String getDnValue(String dn) { return dn.split(",")[1]; }

    @Getter private List<LdapAttribute> attributes;
    @Getter private List<LdapAttributeDelta> deltas;

    public LdapEntity clean(LdapContext context) {
        deltas.clear();
        setLdapContext(context);
        return this;
    }

    public List<String> get(String attrName) {
        final List<String> found = new ArrayList<>();
        for (LdapAttribute attr : attributes) {
            if (attr.getName().equalsIgnoreCase(attrName)) found.add(attr.getValue());
        }
        return found.isEmpty() ? null : found;
    }

    public String getSingle (String attrName) {
        final List<String> found = get(attrName);
        return found.isEmpty() ? null : found.size() == 1 ? found.get(0) : (String) die("getSingle: multiple values found for " + attrName + ": " + found);
    }

    public LdapEntity set(String name, String value) {
        final LdapAttribute attribute = new LdapAttribute(name, value);
        LdapAttribute found = null;
        for (LdapAttribute attr : attributes) {
            if (attr.getName().equalsIgnoreCase(name)) {
                if (found != null) die("set: multiple values found for "+name);
                found = attr;
            }
        }
        if (found == null) {
            attributes.add(attribute);
            deltas.add(new LdapAttributeDelta(attribute, LdapOperation.add));
        } else {
            found.setValue(value);
            deltas.add(new LdapAttributeDelta(attribute, LdapOperation.replace));
        }
        return this;
    }

    public LdapEntity append(String name, String value) {
        final LdapAttribute attribute = new LdapAttribute(name, value);
        attributes.add(attribute);
        deltas.add(new LdapAttributeDelta(attribute, LdapOperation.add));
        return this;
    }

    public void remove(String name) {
        boolean found = false;
        for (Iterator<LdapAttribute> i = attributes.iterator(); i.hasNext(); ) {
            LdapAttribute attr = i.next();
            if (attr.getName().equalsIgnoreCase(name)) {
                i.remove();
                found = true;
            }
        }
        if (found) deltas.add(new LdapAttributeDelta(new LdapAttribute(name), LdapOperation.delete));
    }

    public void remove(String name, String value) {
        for (Iterator<LdapAttribute> i = attributes.iterator(); i.hasNext(); ) {
            LdapAttribute attr = i.next();
            if (attr.getName().equalsIgnoreCase(name) && attr.getValue().equals(value)) {
                i.remove();
                deltas.add(new LdapAttributeDelta(new LdapAttribute(name, value), LdapOperation.delete));
            }
        }
    }

    public String ldifCreate() {
        final StringBuilder b = startLdif();
        for (LdapAttribute attr : attributes) {
            b.append(attr.getName()).append(": ").append(attr.getValue());
        }
        return b.toString();
    }

    public String ldifModify() {
        final StringBuilder b = startLdif();
        b.append("changetype: modify\n");
        boolean firstOperation = true;
        for (LdapAttributeDelta delta : deltas) {
            if (!firstOperation) b.append("-\n");
            firstOperation = false;
            final String attrName = delta.getAttribute().getName();
            switch (delta.getOperation()) {
                case add:
                    b.append("add: ").append(attrName).append("\n")
                            .append(attrName).append(": ").append(delta.getAttribute().getValue()).append("\n");
                    break;

                case replace:
                    b.append("replace: ").append(attrName).append("\n")
                            .append(attrName).append(": ").append(delta.getAttribute().getValue()).append("\n");
                    break;

                case delete:
                    b.append("delete: ").append(attrName).append("\n");
                    if (delta.getAttribute().hasValue()) {
                        b.append(attrName).append(": ").append(delta.getAttribute().getValue()).append("\n");
                    }
                    break;

                default:
                    notSupported("ldifModify: " + delta.getOperation());
            }
        }
        return b.toString();
    }

    public String ldifDelete() {
        final StringBuilder b = startLdif();
        b.append("changetype: delete\n");
        return b.toString();
    }

    private StringBuilder startLdif() { return new StringBuilder("dn: ").append(getDn()).append("\n"); }

    @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
    public class LdapAttribute {
        @Getter @Setter private String name;
        @Getter @Setter private String value;
        public LdapAttribute (String name) { this.name = name; }
        public boolean hasValue () { return !empty(value); }
    }

    public enum LdapOperation { add, replace, delete }

    @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
    public class LdapAttributeDelta {
        @Getter @Setter private LdapAttribute attribute;
        @Getter @Setter private LdapOperation operation;

    }
}
