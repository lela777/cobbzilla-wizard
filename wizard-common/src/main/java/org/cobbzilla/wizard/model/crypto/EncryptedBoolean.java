package org.cobbzilla.wizard.model.crypto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.wizard.model.crypto.EncryptedTypes.ENCRYPTED_STRING;
import static org.cobbzilla.wizard.model.crypto.EncryptedTypes.ENC_PAD;

@Embeddable @NoArgsConstructor @Accessors(chain=true)
public class EncryptedBoolean {

    public static final int COLUMN_MAXLEN = 20 + ENC_PAD;

    public EncryptedBoolean (boolean val) { set(val); }

    private EncryptedBoolean set(boolean val) { return val ? setTrue() : setFalse(); }

    @Type(type=ENCRYPTED_STRING) @Column(columnDefinition="varchar("+ COLUMN_MAXLEN +") NOT NULL")
    @Getter @Setter private String flag;

    public static final String TRUE_SUFFIX = "_true";
    public static final String FALSE_SUFFIX = "_false";

    public EncryptedBoolean setTrue  () { flag = randomAlphanumeric(10) + TRUE_SUFFIX;  return this; }
    public EncryptedBoolean setFalse () { flag = randomAlphanumeric(10) + FALSE_SUFFIX; return this; }

    @Transient public boolean isTrue () { return flag != null && flag.endsWith(TRUE_SUFFIX); }

}