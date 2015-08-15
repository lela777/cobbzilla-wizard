package org.cobbzilla.wizard.validation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class SimpleViolationException extends RuntimeException {

    @Getter private final String messageTemplate;
    @Getter private final String message;
    @Getter private final String invalidValue;

    public SimpleViolationException (String messageTemplate) {
        this(messageTemplate, messageTemplate, null);
    }

    public SimpleViolationException (String messageTemplate, String message) {
        this(messageTemplate, message, null);
    }

    @JsonIgnore public ConstraintViolationBean getBean () {
        return new ConstraintViolationBean(messageTemplate, message, invalidValue);
    }
}
