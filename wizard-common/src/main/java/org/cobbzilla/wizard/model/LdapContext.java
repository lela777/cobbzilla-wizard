package org.cobbzilla.wizard.model;

public interface LdapContext {

    public String getUser_dn();

    String getUser_email();

    String getUser_firstname();

    String getUser_lastname();

    String getUser_mobilePhone();

    String getUser_mobilePhoneCountryCode();

    String getUser_admin();

    String getUser_suspended();

    String getUser_twoFactor();

    String getUser_lastLogin();

    String getUser_locale();

    String getUser_storageQuota();
}
