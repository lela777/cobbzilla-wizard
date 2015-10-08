/**
 * Defines Jasypt data type for transparent hibernate encryption
 */
@TypeDef(
        name=ENCRYPTED_STRING,
        typeClass=EncryptedStringType.class,
        parameters={
                @Parameter(name="encryptorRegisteredName", value=STRING_ENCRYPTOR_NAME)
        }
)

package org.cobbzilla.wizard.model;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.jasypt.hibernate4.type.EncryptedStringType;

import static org.cobbzilla.wizard.model.EncryptedTypes.ENCRYPTED_STRING;
import static org.cobbzilla.wizard.model.EncryptedTypes.STRING_ENCRYPTOR_NAME;

