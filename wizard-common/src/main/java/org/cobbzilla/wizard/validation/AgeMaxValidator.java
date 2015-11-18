package org.cobbzilla.wizard.validation;

import org.joda.time.format.DateTimeFormat;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class AgeMaxValidator implements ConstraintValidator<AgeMax, Object> {

    private long max;
    private String format;
    private boolean emptyOk;

    @Override public void initialize(AgeMax constraintAnnotation) {
        this.max = constraintAnnotation.max();
        this.format = constraintAnnotation.format();
        this.emptyOk = constraintAnnotation.emptyOk();
    }

    @Override public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (empty(value)) return emptyOk;
        if (value instanceof Number) {
            return System.currentTimeMillis() - ((Number) value).longValue() <= max;
        } else if (!empty(format) && value instanceof String) {
            return System.currentTimeMillis() - DateTimeFormat.forPattern(format).parseMillis(value.toString()) <= max;
        }
        return isValid(Long.parseLong(value.toString()), context);
    }
}
