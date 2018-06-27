package org.cobbzilla.wizard.model.entityconfig.validation;

import org.cobbzilla.wizard.model.entityconfig.EntityConfigFieldValidator;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.validation.ValidationResult;

import java.util.Locale;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class EntityConfigFieldValidator_email implements EntityConfigFieldValidator {

    final static String emailRegex = "^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";

    @Override public ValidationResult validate(Locale locale, EntityFieldConfig fieldConfig, Object value) {
        ValidationResult validation = new ValidationResult();
        final String val = empty(value) ? "" : value.toString().trim();
        if (!val.matches(emailRegex)) validation.addViolation("err."+fieldConfig.getName()+".notEmail");
        return validation;
    }

    @Override public Object toObject(Locale locale, String value) {
        return empty(value) ? "" : value.toString().trim();
    }

}
