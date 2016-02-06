package org.cobbzilla.wizard.validation;

import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;

// forked from dropwizard-- https://github.com/codahale/dropwizard

@NoArgsConstructor
public class ValidationResult {

    private static final Transformer BEAN_XFORM = new Transformer() {
        @Override public Object transform(Object input) {
            return new ConstraintViolationBean((ConstraintViolation) input);
        }
    };

    private List<ConstraintViolation> violations = new ArrayList<>();
    private List<ConstraintViolationBean> beans = new ArrayList<>();

    public ValidationResult (List<ConstraintViolation> violations) {
        this.violations.addAll(violations);
    }

    public List<ConstraintViolation> getViolations() { return violations; }

    public void addViolation(ConstraintViolation violation) { violations.add(violation); }
    public void addViolation(ConstraintViolationBean violation) { beans.add(violation); }

    public void addViolation(String messageTemplate) { addViolation(messageTemplate, null, null); }

    public void addViolation(String messageTemplate, String message) { addViolation(messageTemplate, message, null); }

    public void addViolation(String messageTemplate, String message, String invalidValue) {
        beans.add(new ConstraintViolationBean(messageTemplate, message, invalidValue));
    }

    public List<ConstraintViolationBean> getViolationBeans() {
        final List<ConstraintViolationBean> beanList = (List<ConstraintViolationBean>) CollectionUtils.collect(violations, BEAN_XFORM);
        beanList.addAll(beans);
        return beanList;
    }

    public boolean isEmpty () { return violations.isEmpty() && beans.isEmpty(); }

    @Override public String toString() { return violations.toString() + (beans.isEmpty() ? "" : ", "+beans.toString()); }

}
