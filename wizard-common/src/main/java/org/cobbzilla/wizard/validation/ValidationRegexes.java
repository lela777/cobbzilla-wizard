package org.cobbzilla.wizard.validation;

import java.util.regex.Pattern;

public class ValidationRegexes {

    public static final Pattern LOGIN_PATTERN = pattern("^[\\w\\-]+$");
    public static final Pattern EMAIL_PATTERN = pattern("^[A-Z0-9][A-Z0-9._%+-]*@[A-Z0-9.-]+\\.[A-Z]{2,6}$");
    public static final Pattern EMAIL_NAME_PATTERN = pattern("^[A-Z0-9][A-Z0-9._%+-]*$");

    public static final Pattern IPv4_PATTERN = pattern("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)){3}$");
    public static final Pattern IPv6_PATTERN = pattern("^(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))$");

    public static final Pattern HOST_PATTERN  = pattern("^([A-Z0-9]|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9])(\\.([A-Z0-9]|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9]))*$");
    public static final Pattern HOST_PART_PATTERN  = pattern("^([A-Z0-9]|[A-Z0-9][A-Z0-9\\-]{0,61}[A-Z0-9])$");
    public static final Pattern PORT_PATTERN  = pattern("^[\\d]{1,5}$");

    public static final Pattern URL_PATTERN   = pattern("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");
    public static final Pattern HTTP_PATTERN  = pattern("^(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");
    public static final Pattern HTTPS_PATTERN = pattern("^https://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]$");

    public static final Pattern FILENAME_PATTERN = pattern("^[_A-Z0-9\\-\\.]+$");
    public static final Pattern INTEGER_PATTERN = pattern("^[0-9]+$");

    private static Pattern pattern(String regex) { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE); }

}
