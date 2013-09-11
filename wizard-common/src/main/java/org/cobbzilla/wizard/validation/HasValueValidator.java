package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Collection;

public class HasValueValidator implements ConstraintValidator<HasValue, Object> {

    @Override
    public void initialize(HasValue constraintAnnotation) {}

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return false;
        if (value instanceof Collection) {
            return !((Collection) value).isEmpty();
        }
        return value.toString().trim().length() > 0;
    }

}
