package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class AgeMinValidator implements ConstraintValidator<AgeMin, Object> {

    private long min;
    private boolean emptyOk;

    @Override public void initialize(AgeMin constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.emptyOk = constraintAnnotation.emptyOk();
    }

    @Override public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (empty(value)) return emptyOk;
        if (value instanceof Number) {
            return System.currentTimeMillis() - ((Number) value).longValue() >= min;
        }
        return isValid(Long.parseLong(value.toString()), context);
    }
}
