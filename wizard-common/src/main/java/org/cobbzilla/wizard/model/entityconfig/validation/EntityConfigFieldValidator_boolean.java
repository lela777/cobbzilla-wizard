package org.cobbzilla.wizard.model.entityconfig.validation;

import org.cobbzilla.wizard.model.entityconfig.EntityConfigFieldValidator;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.validation.ValidationResult;

import java.util.Locale;

public class EntityConfigFieldValidator_boolean implements EntityConfigFieldValidator {

    @Override public ValidationResult validate(Locale locale, EntityFieldConfig fieldConfig, Object value) {
        return null;
    }

    @Override public Object toObject(Locale locale, String value) {
        return Boolean.valueOf(value);
    }

}
