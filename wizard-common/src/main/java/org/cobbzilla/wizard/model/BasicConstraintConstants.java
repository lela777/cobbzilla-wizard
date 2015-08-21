package org.cobbzilla.wizard.model;

public class BasicConstraintConstants {

    public static final String ERR_HASHED_PASSWORD_EMPTY = "{err.hashedPassword.empty}";
    public static final String ERR_HASHED_PASSWORD_LENGTH = "{err.hashedPassword.length}";

    public static final String ERR_UUID_UNIQUE = "{err.uuid.unique}";
    public static final String ERR_UUID_EMPTY = "{err.uuid.empty}";
    public static final String ERR_UUID_LENGTH = "{err.uuid.length}";

    public static final String ERR_SORT_ORDER_INVALID = "{err.sortOrder.invalid}";

    public static final String ERR_RESET_TOKEN_LENGTH = "{err.resetToken.length}";
    public static final String ERR_BOUNDS_INVALID = "{err.bounds.invalid}";

    public static final int UUID_MAXLEN = 100;
    public static final int HASHEDPASSWORD_MAXLEN = 200;
    public static final int RESETTOKEN_MAXLEN = 30;

    public static final int URL_MAXLEN = 1024;

    public static final String SV_MAJOR_LENGTH = "{err.semanticVersion.major.length}";
    public static final String SV_MINOR_LENGTH = "{err.semanticVersion.minor.length}";
    public static final String SV_PATCH_LENGTH = "{err.semanticVersion.patch.length}";
    public static final int SV_VERSION_MAXLEN = 30;
}
