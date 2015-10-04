package org.cobbzilla.wizard.model.ldap;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor @EqualsAndHashCode(of="javaName")
public class LdapAttributeType {

    @Getter @Setter private String javaName;
    @Getter @Setter private String ldapName;
    @Getter @Setter private boolean multiple = false;

    public LdapAttributeType(String javaName, String ldapName) {
        this.javaName = javaName;
        this.ldapName = ldapName;
    }

    public String toString() { return "java:" + javaName + "/ldap:" + ldapName + "/multiple:" + multiple; }

}
