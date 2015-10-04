package org.cobbzilla.wizard.model.ldap;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class LdapAttribute {

    @Getter @Setter private String name;
    @Getter @Setter private String value;

    public LdapAttribute(String name) { this.name = name; }

    public boolean hasValue() { return !empty(value); }

    public boolean isName(String name) { return this.name.equalsIgnoreCase(name); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LdapAttribute that = (LdapAttribute) o;
        return isName(that.name);
    }

    @Override public int hashCode() { return name.toLowerCase().hashCode(); }

    @Override public String toString() { return hasValue() ? name + ": " + value : name; }

}
