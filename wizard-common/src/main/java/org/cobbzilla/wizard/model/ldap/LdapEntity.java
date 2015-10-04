package org.cobbzilla.wizard.model.ldap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.UniquelyNamedEntity;

import javax.persistence.Transient;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.wizard.ldap.LdapUtil.getFirstDnLabel;
import static org.cobbzilla.wizard.ldap.LdapUtil.getFirstDnValue;

/**
 * Abstract LdapEntity. Encapsulates logic for storing a DN, a base location, and arbitrary attributes.
 * Requires an LdapContext to know how to map data to LDAP attribute names.
 * This means you need to add "ldapContext" to the Jackson ObjectMapper that Jersey will use.
 */
@NoArgsConstructor @Accessors(chain=true) @Slf4j
public abstract class LdapEntity extends UniquelyNamedEntity {

    public static final String LDAP_CONTEXT = "ldapContext";

    public LdapEntity (LdapEntity other) {
        setLdapContext(other.getLdapContext());
        setDn(other.getDn());
        setAttributes(other.getAttributes());
        clean();
    }

    public LdapEntity merge (LdapEntity other) {
        if (empty(getDn())) setDn(other.getDn());
        final List<LdapAttribute> attributes = getAttributes();
        for (LdapAttribute attr : other.getAttributes()) {
            if (attributes.contains(attr)) {
                final LdapAttributeType type = typeForLdap(attr.getName());
                if (type.isMultiple()) {
                    append(attr.getName(), attr.getValue());
                } else {
                    set(attr.getName(), attr.getValue());
                }
            } else {
                append(attr.getName(), attr.getValue());
            }
        }
        return this;
    }

    @Getter protected String dn;
    public LdapEntity setDn(String dn) {
        if (dn != null) {
            final String label = getFirstDnLabel(dn);
            if (label.equals(getIdField())) {
                set(getIdField(), getFirstDnValue(dn));
            } else {
                die("setDn: invalid DN (expected prefix="+getIdField()+"): "+dn);
            }
        }
        this.dn = dn;
        return this;
    }

    @JsonIgnore @Transient @Getter @Setter protected LdapContext ldapContext;

    // convenience method
    public LdapContext ldap() { return getLdapContext(); }

    @Override public String getUuid () { return getSingle(ldap().getExternal_id()); }
    @Override public void setUuid (String uuid) { set(ldap().getExternal_id(), uuid);  }

    public String getName () { return getDnValue(getDn()).toLowerCase(); }
    public UniquelyNamedEntity setName (String name) {
        return getLdapContext() == null ? this : setDn(getIdField() + "=" + name.toLowerCase() + "," + getParentDn());
    }

    @JsonIgnore public abstract String getIdField();
    @JsonIgnore public abstract String getParentDn();

    @Getter(lazy=true, value=AccessLevel.PROTECTED) private final List<LdapAttributeType> ldapTypes = initLdapTypes();
    protected List<LdapAttributeType> initLdapTypes () { return Collections.emptyList(); }

    @Getter(lazy=true) private final Map<String, LdapAttributeType> typesByJavaName = initTypesByJavaName();
    private Map<String, LdapAttributeType> initTypesByJavaName() {
        final Map<String, LdapAttributeType> map = new HashMap<>();
        for (LdapAttributeType t : getLdapTypes()) map.put(t.getJavaName(), t);
        return map;
    }

    @Getter(lazy=true) private final Map<String, LdapAttributeType> typesByLdapName = initTypesByLdapName();
    private Map<String, LdapAttributeType> initTypesByLdapName() {
        final Map<String, LdapAttributeType> map = new HashMap<>();
        for (LdapAttributeType t : getLdapTypes()) map.put(t.getLdapName(), t);
        return map;
    }

    public LdapAttributeType typeForLdap(String ldapName) {
        final LdapAttributeType type = getTypesByLdapName().get(ldapName);
        return type != null ? type : new LdapAttributeType(ldapName, ldapName);
    }

    public LdapAttributeType typeForJava(String javaName) {
        final LdapAttributeType type = getTypesByJavaName().get(javaName);
        return type != null ? type : new LdapAttributeType(javaName, javaName);
    }

    @JsonIgnore protected String getDnValue(String dn) { return getFirstDnValue(dn); }

    @JsonIgnore @Getter @Setter private List<LdapAttribute> attributes = new ArrayList<>();
    @JsonIgnore @Getter private List<LdapAttributeDelta> deltas = new ArrayList<>();

    public boolean hasAttribute (String attrName) {
        for (LdapAttribute attr : attributes) if (attr.isName(attrName)) return true;
        return false;
    }

    public LdapEntity clean () { deltas.clear(); return this; }

    public List<String> get(String attrName) {
        final List<String> found = new ArrayList<>();
        for (LdapAttribute attr : attributes) {
            if (attr.isName(attrName)) found.add(attr.getValue());
        }
        return found.isEmpty() ? null : found;
    }

    public String getSingle (String attrName) {
        final List<String> found = get(attrName);
        return empty(found) ? null : found.size() == 1 ? found.get(0) : (String) die("getSingle: multiple values found for " + attrName + ": " + found);
    }

    public LdapEntity set(String name, String value) {
        final LdapAttribute attribute = new LdapAttribute(name, value);
        LdapAttribute found = null;
        for (LdapAttribute attr : attributes) {
            if (attr.isName(name)) {
                if (found != null) die("set: multiple values found for "+name);
                found = attr;
            }
        }
        if (found == null && value != null) {
            attributes.add(attribute);
            deltas.add(new LdapAttributeDelta(attribute, LdapOperation.add));

        } else if (found != null && !found.getValue().equals(value)) {
            if (empty(value)) {
                log.warn("not removing field by setting empty value: " + name);
            } else {
                // remove + add works because LdapAttribute.equals operates on name only
                attributes.remove(attribute);
                attributes.add(attribute);
                deltas.add(new LdapAttributeDelta(attribute, LdapOperation.replace));
            }

        } else {
            log.debug("set("+name+"="+value+"): value unchanged, no delta");
        }
        return this;
    }

    public LdapEntity append(String name, String value) {
        // id field must be single-valued
        if (name.equals(getIdField())) return set(name, value);

        final LdapAttribute attribute = new LdapAttribute(name, value);
        attributes.add(attribute);
        deltas.add(new LdapAttributeDelta(attribute, LdapOperation.add));
        return this;
    }

    public LdapEntity appendAll(String name, List<String> values) {
        for (String value : values) append(name, value);
        return this;
    }

    public void remove(String name) {
        boolean found = false;
        for (Iterator<LdapAttribute> i = attributes.iterator(); i.hasNext(); ) {
            LdapAttribute attr = i.next();
            if (attr.isName(name)) {
                i.remove();
                found = true;
            }
        }
        if (found) deltas.add(new LdapAttributeDelta(new LdapAttribute(name), LdapOperation.delete));
    }

    public void remove(String name, String value) {
        for (Iterator<LdapAttribute> i = attributes.iterator(); i.hasNext(); ) {
            LdapAttribute attr = i.next();
            if (attr.isName(name) && attr.getValue().equals(value)) {
                i.remove();
                deltas.add(deleted(name, value));
            }
        }
    }

    private LdapAttributeDelta deleted(String name, String value) {
        return new LdapAttributeDelta(new LdapAttribute(name, value), LdapOperation.delete);
    }

    private static Map<String, Comparator<LdapEntity>> comparatorCache = new ConcurrentHashMap<>();

    public static Comparator<LdapEntity> comparator (final String field, ResultPage.SortOrder order) {
        final ResultPage.SortOrder sort = order == null ? ResultPage.SortOrder.ASC : order;
        final String cacheKey = field + ":" + order;
        Comparator<LdapEntity> comp = comparatorCache.get(cacheKey);
        if (comp == null) {
            comp = new Comparator<LdapEntity>() {
                @Override public int compare(LdapEntity o1, LdapEntity o2) {
                    final Object v1 = ReflectionUtil.get(o1, field);
                    final Object v2 = ReflectionUtil.get(o2, field);
                    switch (sort) {
                        case ASC:
                            if (v1 == null) return v2 == null ? 0 : -1;
                            return v2 == null ? 1 : v1.toString().compareTo(v2.toString());

                        case DESC:
                            if (v2 == null) return v1 == null ? 0 : -1;
                            return v1 == null ? 1 : v2.toString().compareTo(v1.toString());

                        default: return notSupported(sort.name());
                    }
                }
            };
            comparatorCache.put(cacheKey, comp);
        }
        return comp;
    }

    public String ldifCreate() {
        final StringBuilder b = startLdif();
        for (LdapAttribute attr : attributes) {
            b.append(attr.getName()).append(": ").append(attr.getValue()).append("\n");
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
            final String value = delta.getAttribute().getValue();
            switch (delta.getOperation()) {
                case add:
                    final LdapAttributeType attrType = typeForLdap(attrName);
                    if (attrType.isMultiple() || !hasAttribute(attrName)) {
                        b.append("add: ").append(attrName).append("\n")
                                .append(attrName).append(": ").append(value).append("\n");
                    } else {
                        b.append("replace: ").append(attrName).append("\n")
                                .append(attrName).append(": ").append(value).append("\n");
                    }
                    break;

                case replace:
                    b.append("replace: ").append(attrName).append("\n")
                            .append(attrName).append(": ").append(value).append("\n");
                    break;

                case delete:
                    b.append("delete: ").append(attrName).append("\n");
                    if (delta.getAttribute().hasValue()) {
                        b.append(attrName).append(": ").append(value).append("\n");
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

    public void attrFromLdif(String name, String value) { append(name, value); }

}
