package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class AgeMaxValidator implements ConstraintValidator<AgeMax, Object> {

    private long max;
    private boolean emptyOk;

    @Override public void initialize(AgeMax constraintAnnotation) {
        this.max = constraintAnnotation.max();
        this.emptyOk = constraintAnnotation.emptyOk();
    }

    @Override public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (empty(value)) return emptyOk;
        if (value instanceof Number) {
            return System.currentTimeMillis() - ((Number) value).longValue() <= max;
        }
        return isValid(Long.parseLong(value.toString()), context);
    }
}
