package org.cobbzilla.wizard.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.system.CommandShell;

import java.util.HashMap;
import java.util.Iterator;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

public class LdapConfiguration extends HashMap<String, String> {

    public void setJson (String json) {
        final JsonNode node = fromJsonOrDie(json, JsonNode.class);
        if (!(node instanceof ObjectNode)) die("Not a JSON object node: "+json);

        final ObjectNode obj = (ObjectNode) node;
        final Iterator<String> fieldNames = obj.fieldNames();
        while (fieldNames.hasNext()) {
            final String name = fieldNames.next();
            put(name, obj.get(name).asText());
        }
    }

    public String getServer() {
        return containsKey("server") ? get("server") : "ldap://127.0.0.1:389";
    }

    public String getScheme () { return URIUtil.getScheme(getServer()); }
    public String getHost () { return URIUtil.getHost(getServer()); }
    public int getPort () { return URIUtil.getPort(getServer()); }

    public String getVersion() {
        return containsKey("version") ? get("version") : "3";
    }

    public String getDomain () {
        return containsKey("domain") ? get("domain") : CommandShell.domainname();
    }

    public String domainify (String value) {
        return "dc="+value.replace(".", ",dc=");
    }

    public String getLdap_domain() {
        return containsKey("ldap_domain") ? get("ldap_domain") : domainify(getDomain());
    }

    public String getRealm() {
        return containsKey("realm") ? get("realm") : getDomain();
    }
    public String getBase_dn() {
        return containsKey("base_dn") ? get("base_dn") : getParent_cn() + getLdap_domain();
    }
    public String getParent_cn() {
        return containsKey("parent_cn") ? get("parent_cn") : "cn=cloudos,";
    }
    public String getAdmin() {
        return containsKey("admin") ? get("admin") : "admin";
    }
    public String getAdmin_dn () {
        return containsKey("admin_dn") ? get("admin_dn") : "cn="+getAdmin()+","+getLdap_domain();
    }

    public String getOrg_unit_class () {
        return containsKey("org_unit_class") ? get("org_unit_class") : "organizationalUnit";
    }

    public String getPassword() { return System.getenv("LDAP_PASSWORD"); }

    public String getExternal_id() {
        return containsKey("external_id") ? get("external_id") : "entryUUID";
    }

    public String getUsers() {
        return containsKey("users") ? get("users") : "People";
    }
    public String getUser_dn () {
        return containsKey("user_dn") ? get("user_dn") : "ou="+getUsers()+","+getBase_dn();
    }

    public String getUser_class() {
        return containsKey("user_class") ? get("user_class") : "inetorgperson";
    }
    public String getUser_filter() {
        return containsKey("user_filter") ? get("user_filter") : "(objectclass="+getUser_class()+")";
    }
    public String getUser_username() {
        return containsKey("user_username") ? get("user_username") : "uid";
    }
    public String getUser_email() {
        return containsKey("user_email") ? get("user_email") : "mail";
    }
    public String getUser_displayname() {
        return containsKey("user_displayname") ? get("user_displayname") : "displayName";
    }
    public String getUser_firstname() {
        return containsKey("user_firstname") ? get("user_firstname") : "givenName";
    }
    public String getUser_lastname() {
        return containsKey("user_lastname") ? get("user_lastname") : "sn";
    }
    public String getUser_groupnames() {
        return containsKey("user_groupnames") ? get("user_groupnames") : "memberOf";
    }
    public String getUser_username_rdn() {
        return containsKey("user_username_rdn") ? get("user_username_rdn") : "cn";
    }
    public String getUser_password() {
        return containsKey("user_password") ? get("user_password") : "userPassword";
    }
    public String getUser_encryption() {
        return containsKey("user_encryption") ? get("user_encryption") : "sha";
    }

    public String getGroups() {
        return containsKey("groups") ? get("groups") : "Groups";
    }
    public String getGroup_dn() {
        return containsKey("group_dn") ? get("group_dn") : "ou="+getGroups()+","+getBase_dn();
    }

    public String getGroup_class() {
        return containsKey("group_class") ? get("group_class") : "groupOfUniqueNames";
    }
    public String getGroup_filter() {
        return containsKey("group_filter") ? get("group_filter") : "(objectclass="+getGroup_class()+")";
    }
    public String getGroup_name() {
        return containsKey("group_name") ? get("group_name") : "cn";
    }
    public String getGroup_description() {
        return containsKey("group_description") ? get("group_description") : "description";
    }
    public String getGroup_usernames() {
        return containsKey("group_usernames") ? get("group_usernames") : "uniqueMember";
    }

    public String userDN (String name) { return getUser_username() + "=" + name + "," + getUser_dn(); }
    public String groupDN (String name) { return getGroup_name() + "="  + name + "," + getGroup_dn(); }

    public String filterGroup(String groupName) {
        return empty(groupName) ? getGroup_filter() : "(&("+ getGroup_filter()+")("+ getGroup_name()+"="+groupName+"))";
    }
}
