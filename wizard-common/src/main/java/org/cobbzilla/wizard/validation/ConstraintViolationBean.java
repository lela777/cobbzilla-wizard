package org.cobbzilla.wizard.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.ConstraintViolation;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement @AllArgsConstructor @NoArgsConstructor
public class ConstraintViolationBean {

    @XmlElement @Getter @Setter private String messageTemplate;
    @XmlElement @Getter @Setter private String message;
    @XmlElement @Getter @Setter private String invalidValue;

    public ConstraintViolationBean(String messageTemplate) {
        this(messageTemplate, messageTemplate, null);
    }

    public ConstraintViolationBean(String messageTemplate, String message) {
        this(messageTemplate, message, null);
    }

    public ConstraintViolationBean(ConstraintViolation violation) {
        this.messageTemplate = violation.getMessageTemplate();
        this.message = violation.getMessage();
        try {
            Object val = violation.getInvalidValue();
            this.invalidValue = (val == null) ? "none-set" : val.toString();
        } catch (Exception e) {
            this.invalidValue = "Error converting invalid value to String: "+e;
        }
    }

}
