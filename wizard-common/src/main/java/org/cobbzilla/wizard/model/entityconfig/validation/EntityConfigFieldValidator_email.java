package org.cobbzilla.wizard.model.entityconfig.validation;

import org.cobbzilla.wizard.model.entityconfig.EntityConfigFieldValidator;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.validation.ValidationResult;

import java.util.Locale;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.ValidationRegexes.EMAIL_PATTERN;

public class EntityConfigFieldValidator_email implements EntityConfigFieldValidator {

    @Override public ValidationResult validate(Locale locale, EntityFieldConfig fieldConfig, Object value) {
        ValidationResult validation = new ValidationResult();
        final String val = empty(value) ? "" : value.toString().trim();
        if (!EMAIL_PATTERN.matcher(val).find()) {
            validation.addViolation("err."+fieldConfig.getName()+".notEmail");
        }
        return validation;
    }

    @Override public Object toObject(Locale locale, String value) {
        return empty(value) ? "" : value.toString().trim();
    }

}
