package org.cobbzilla.wizard.validation;

import lombok.*;
import org.cobbzilla.util.json.JsonUtil;

import javax.validation.ConstraintViolation;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.cobbzilla.util.string.StringUtil.empty;

@XmlRootElement @AllArgsConstructor @NoArgsConstructor @ToString
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

    public static List<ConstraintViolationBean> fromJsonArray(String json) {
        return empty(json)
                ? (List<ConstraintViolationBean>) Collections.EMPTY_LIST
                : Arrays.asList(JsonUtil.fromJsonOrDie(json, ConstraintViolationBean[].class));
    }

}
