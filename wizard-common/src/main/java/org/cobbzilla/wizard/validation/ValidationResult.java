package org.cobbzilla.wizard.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @JsonIgnore public List<ConstraintViolation> getViolations() { return violations; }

    public void addViolation(ConstraintViolation violation) { violations.add(violation); }
    public void addViolation(ConstraintViolationBean violation) { beans.add(violation); }

    public void addViolation(String messageTemplate) { addViolation(messageTemplate, null, null); }

    public void addViolation(String messageTemplate, String message) { addViolation(messageTemplate, message, null); }

    public void addViolation(String messageTemplate, String message, String invalidValue) {
        final ConstraintViolationBean err = new ConstraintViolationBean(messageTemplate, message, invalidValue);
        for (ConstraintViolationBean bean : beans) {
            if (bean.equals(err)) {
                return; // already exists
            }
        }
        beans.add(err);
    }

    public void addAll(ValidationResult result) {
        for (ConstraintViolationBean violationBean : result.getViolationBeans()) {
            addViolation(violationBean);
        }
    }

    public List<ConstraintViolationBean> getViolationBeans() {
        final List<ConstraintViolationBean> beanList = (List<ConstraintViolationBean>) CollectionUtils.collect(violations, BEAN_XFORM);
        beanList.addAll(beans);
        return beanList;
    }
    public void setViolationBeans (List<ConstraintViolationBean> beans) {
        this.beans = beans;
    }

    @JsonIgnore public boolean isValid () { return isEmpty(); }
    @JsonIgnore public boolean isInvalid () { return !isEmpty(); }
    @JsonIgnore public boolean isEmpty () { return violations.isEmpty() && beans.isEmpty(); }

    public boolean hasFieldError(String name) {
        for (ConstraintViolationBean bean : getViolationBeans()) {
            final String field = ConstraintViolationBean.getField(bean.getMessageTemplate());
            if (field != null && name.equals(field)) return true;
        }
        return false;
    }

    public boolean hasInvalidValue (String value) {
        for (ConstraintViolationBean bean : getViolationBeans()) {
            if (bean.hasInvalidValue() && bean.getInvalidValue().equals(value)) return true;
        }
        return false;
    }

    @Override public String toString() { return violations.toString() + (beans.isEmpty() ? "" : ", "+beans.toString()); }

    public ValidationErrors errors() { return new ValidationErrors(this.getViolationBeans()); }

    public int violationCount() { return isEmpty() ? 0 : getViolationBeans().size(); }

}
