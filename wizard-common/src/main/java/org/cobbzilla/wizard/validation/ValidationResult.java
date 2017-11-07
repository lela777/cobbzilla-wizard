package org.cobbzilla.wizard.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import javax.validation.ConstraintViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// forked from dropwizard-- https://github.com/codahale/dropwizard

@NoArgsConstructor
public class ValidationResult {

    private static final Transformer BEAN_XFORM = input -> new ConstraintViolationBean((ConstraintViolation) input);

    private final AtomicReference<List<ConstraintViolation>> violations = new AtomicReference<>(new ArrayList<>());
    private final AtomicReference<List<ConstraintViolationBean>> beans = new AtomicReference<>(new ArrayList<>());

    public ValidationResult (List<ConstraintViolation> violations) {
        synchronized (this.violations) { this.violations.get().addAll(violations); }
    }

    @JsonIgnore public List<ConstraintViolation> getViolations() { return violations.get(); }

    public void addViolation(ConstraintViolation violation) { synchronized (violations) { violations.get().add(violation); } }
    public void addViolation(ConstraintViolationBean violation) { synchronized (beans) { beans.get().add(violation); } }

    public void addViolation(String messageTemplate) { addViolation(messageTemplate, null, null); }

    public void addViolation(String messageTemplate, String message) { addViolation(messageTemplate, message, null); }

    public void addViolation(String messageTemplate, String message, String invalidValue) {
        final ConstraintViolationBean err = new ConstraintViolationBean(messageTemplate, message, invalidValue);
        synchronized (beans) {
            for (ConstraintViolationBean bean : beans.get()) {
                if (bean.equals(err)) {
                    return; // already exists
                }
            }
            beans.get().add(err);
        }
    }

    public void addAll(ValidationResult result) {
        for (ConstraintViolationBean violationBean : result.getViolationBeans()) {
            addViolation(violationBean);
        }
    }

    public List<ConstraintViolationBean> getViolationBeans() {
        final List<ConstraintViolationBean> beanList = (List<ConstraintViolationBean>) CollectionUtils.collect(violations.get(), BEAN_XFORM);
        beanList.addAll(beans.get());
        return beanList;
    }
    public void setViolationBeans (List<ConstraintViolationBean> beans) {
        synchronized (this.beans) { this.beans.set(beans == null ? new ArrayList<>() : beans); }
    }

    @JsonIgnore public boolean isValid () { return isEmpty(); }
    @JsonIgnore public boolean isInvalid () { return !isEmpty(); }
    @JsonIgnore public boolean isEmpty () { return violations.get().isEmpty() && beans.get().isEmpty(); }

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

    @Override public String toString() { return violations.get().toString() + (beans.get().isEmpty() ? "" : ", "+beans.get().toString()); }

    public ValidationErrors errors() { return new ValidationErrors(this.getViolationBeans()); }

    public int violationCount() { return isEmpty() ? 0 : getViolationBeans().size(); }

}
