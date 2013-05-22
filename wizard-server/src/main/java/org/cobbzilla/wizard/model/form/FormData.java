package org.cobbzilla.wizard.model.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.form.FormConstraintConstants.*;

@Embeddable
public class FormData {

    @Size(max=PMR_VALUE_MAXLEN, message= ERR_FIELD_VALUE_LENGTH)
    @Column(length= PMR_VALUE_MAXLEN)
    @Getter @Setter private String value;

    @Size(max=PMR_DESCRIPTION_MAXLEN, message= ERR_FIELD_DESCRIPTION_LENGTH)
    @Column(length=PMR_DESCRIPTION_MAXLEN)
    @Getter @Setter private String description;

    @JsonIgnore
    @Transient
    public boolean isEmpty() {
        return StringUtils.isEmpty(value) && StringUtils.isEmpty(description);
    }

}
