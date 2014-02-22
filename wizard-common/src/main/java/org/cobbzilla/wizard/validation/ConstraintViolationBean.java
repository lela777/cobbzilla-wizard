package org.cobbzilla.wizard.validation;

import lombok.Getter;
import lombok.Setter;

import javax.validation.ConstraintViolation;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConstraintViolationBean {

    @XmlElement @Getter @Setter private String messageTemplate;
    @XmlElement @Getter @Setter private String message;
    @XmlElement @Getter @Setter private String invalidValue;

    public ConstraintViolationBean() {}

    public ConstraintViolationBean(String messageTemplate, String message, String invalidValue) {
        this.messageTemplate = messageTemplate;
        this.message = message;
        this.invalidValue = invalidValue;
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
