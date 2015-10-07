/**
 * Defines Jasypt data type for transparent hibernate encryption
 */
@TypeDef(
        name="encryptedString",
        typeClass=EncryptedStringType.class,
        parameters={
                @Parameter(name="encryptorRegisteredName", value="hibernateStringEncryptor")
        }
)

package org.cobbzilla.wizard.spring.config.rdbms;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.jasypt.hibernate4.type.EncryptedStringType;