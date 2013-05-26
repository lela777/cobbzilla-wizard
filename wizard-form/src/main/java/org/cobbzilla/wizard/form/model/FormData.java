package org.cobbzilla.wizard.form.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;

@Embeddable
public class FormData {

    @Size(max=FIELD_DATA_VALUE_MAXLEN, message=ERR_FIELD_VALUE_LENGTH)
    @Column(length=FIELD_DATA_VALUE_MAXLEN)
    @Getter @Setter private String value;

    @Size(max=FIELD_DATA_DESCRIPTION_MAXLEN, message=ERR_FIELD_DESCRIPTION_LENGTH)
    @Column(length=FIELD_DATA_DESCRIPTION_MAXLEN)
    @Getter @Setter private String description;

    @JsonIgnore
    @Transient
    public boolean isEmpty() {
        return StringUtils.isEmpty(value) && StringUtils.isEmpty(description);
    }

}
