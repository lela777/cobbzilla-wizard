package org.cobbzilla.wizard.model;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.jasypt.hibernate4.type.EncryptedStringType;

@TypeDef(
        name=EncryptedTypes.ENCRYPTED_STRING,
        typeClass=EncryptedStringType.class,
        parameters={
                @Parameter(name="encryptorRegisteredName", value=EncryptedTypes.STRING_ENCRYPTOR_NAME)
        }
)
public class EncryptedTypes {

        public static final String ENCRYPTED_STRING = "encryptedString";
        public static final String STRING_ENCRYPTOR_NAME = "hibernateStringEncryptor";

}
