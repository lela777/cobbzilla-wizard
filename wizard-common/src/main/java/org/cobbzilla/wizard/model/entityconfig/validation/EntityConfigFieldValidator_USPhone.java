package org.cobbzilla.wizard.model.entityconfig.validation;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.entityconfig.EntityConfigFieldValidator;
import org.cobbzilla.wizard.model.entityconfig.EntityFieldConfig;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.cobbzilla.wizard.validation.Validator;

import javax.swing.text.MaskFormatter;
import java.text.ParseException;
import java.util.Locale;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class EntityConfigFieldValidator_USPhone implements EntityConfigFieldValidator {
    private static final int DEFAULT_LENGTH = 10;
    private static final MaskFormatter US_PHONE_MASK = buildUSPhoneMask();

    private static MaskFormatter buildUSPhoneMask() {
        try {
            return new MaskFormatter("(###) ###-####");
        } catch (ParseException e) {
            return die("invalid phone mask in code", e);
        }
    }

    @Override public ValidationResult validate(Locale locale, Validator validator, EntityFieldConfig fieldConfig,
                                               Object value) {
        if (empty(value)) return null;

        final int expectedLength = fieldConfig.hasLength() ? fieldConfig.getLength() : DEFAULT_LENGTH;
        if (stripPhoneString(value).length() == expectedLength) return null;

        return new ValidationResult("err." + fieldConfig.getName() + ".length");
    }

    @Override public Object toObject(Locale locale, String value) {
        if (empty(value)) return "";

        final String strippedValue = stripPhoneString(value);
        try {
            return US_PHONE_MASK.valueToString(strippedValue);
        } catch (ParseException e) {
            log.error("invalid phone number: " + value + " stripped to: " + strippedValue, e);
            return "";
        }
    }

    private String stripPhoneString(@NonNull final Object value) {
        return value.toString().replaceAll("[\\s+().-]", "");
    }
}
