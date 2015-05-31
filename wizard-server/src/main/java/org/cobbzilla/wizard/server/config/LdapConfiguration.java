package org.cobbzilla.wizard.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.system.CommandShell;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

public class LdapConfiguration {

    private Map<String, String> config = new HashMap<>();

    public void setJson (String json) {
        // yaml escapes the embedded quotation marks for some reason so we un-escape them here.
        // Please don't use &quot; within any LDAP config values :)
        if (json.startsWith("{&quot;")) json = json.replace("&quot;", "\"");

        final JsonNode node = fromJsonOrDie(json, JsonNode.class);
        if (!(node instanceof ObjectNode)) die("Not a JSON object node: "+json);

        final ObjectNode obj = (ObjectNode) node;
        final Iterator<String> fieldNames = obj.fieldNames();
        while (fieldNames.hasNext()) {
            final String name = fieldNames.next();
            config.put(name, obj.get(name).asText());
        }
    }

    private String val (String field, String defaultValue) {
        final String value = config.get(field);
        return !empty(value) ? value : defaultValue;
    }

    public String domainify (String value) { return "dc="+value.replace(".", ",dc="); }

    public String getServer() { return val("server", "ldap://127.0.0.1:389"); }

    public String getScheme () { return URIUtil.getScheme(getServer()); }
    public String getHost () { return URIUtil.getHost(getServer()); }
    public int getPort () { return URIUtil.getPort(getServer()); }

    public String getVersion() { return val("version", "3"); }
    public String getDomain () { return val("domain", CommandShell.domainname()); }
    public String getLdap_domain() { return val("ldap_domain", domainify(getDomain())); }
    public String getRealm() { return val("realm", getDomain()); }
    public String getBase_dn() { return val("base_dn", getParent_cn() + getLdap_domain()); }
    public String getParent_cn() { return val("parent_cn", "cn=cloudos,"); }

    public String getAdmin() { return val("admin", "admin"); }
    public String getAdmin_dn () { return val("admin_dn", "cn="+getAdmin()+","+getLdap_domain()); }
    public String getPassword() { return System.getenv("LDAP_PASSWORD"); }

    public String getOrg_unit_class () { return val("org_unit_class", "organizationalUnit"); }
    public String getExternal_id() { return val("external_id", "entryUUID"); }

    public String getUsers() { return val("users", "People"); }
    public String getUser_dn () { return val("user_dn", "ou="+getUsers()+","+getBase_dn()); }
    public String getUser_class() { return val("user_class", "inetorgperson"); }
    public String getUser_filter() { return val("user_filter", "(objectclass="+getUser_class()+")"); }
    public String getUser_username() { return val("user_username", "uid"); }
    public String getUser_email() { return val("user_email", "mail"); }
    public String getUser_displayname() { return val("user_displayname", "displayName"); }
    public String getUser_firstname() { return val("user_firstname", "givenName"); }
    public String getUser_lastname() { return val("user_lastname", "sn"); }
    public String getUser_groupnames() { return val("user_groupnames", "memberOf"); }
    public String getUser_username_rdn() { return val("user_username_rdn", "cn"); }
    public String getUser_password() { return val("user_password", "userPassword"); }
    public String getUser_encryption() { return val("user_encryption", "sha"); }

    public String getGroups() { return val("groups", "Groups"); }
    public String getGroup_dn() { return val("group_dn", "ou="+getGroups()+","+getBase_dn()); }
    public String getGroup_class() { return val("group_class", "groupOfUniqueNames"); }
    public String getGroup_filter() { return val("group_filter", "(objectclass="+getGroup_class()+")"); }
    public String getGroup_name() { return val("group_name", "cn"); }
    public String getGroup_description() { return val("group_description", "description"); }
    public String getGroup_usernames() { return val("group_usernames", "uniqueMember"); }

    public String userDN (String name) { return getUser_username() + "=" + name + "," + getUser_dn(); }
    public String groupDN (String name) { return getGroup_name() + "="  + name + "," + getGroup_dn(); }

    public String filterGroup(String groupName) {
        return empty(groupName) ? getGroup_filter() : "(&("+ getGroup_filter()+")("+ getGroup_name()+"="+groupName+"))";
    }
}
