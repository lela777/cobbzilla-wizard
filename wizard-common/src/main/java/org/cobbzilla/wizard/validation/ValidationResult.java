package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;

// forked from dropwizard-- https://github.com/codahale/dropwizard

public class ValidationResult {

    private List<ConstraintViolation> violations = new ArrayList<>();

    public ValidationResult (List<ConstraintViolation> violations) {
        this.violations.addAll(violations);
    }

    public List<ConstraintViolation> getViolations() { return violations; }

    public boolean isEmpty () { return violations.isEmpty(); }

    @Override public String toString() { return violations.toString(); }

}
