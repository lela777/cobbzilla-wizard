package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class HasValueValidator implements ConstraintValidator<HasValue, String> {

    @Override
    public void initialize(HasValue constraintAnnotation) {}

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.trim().length() > 0;
    }

}
