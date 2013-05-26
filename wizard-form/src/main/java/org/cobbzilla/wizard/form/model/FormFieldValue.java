package org.cobbzilla.wizard.form.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;

@Entity
public class FormFieldValue extends IdentifiableBase implements Identifiable {

    @Size(max=FORM_OWNER_MAXLEN, message=ERR_FORM_OWNER_LENGTH)
    @Column(length=FORM_OWNER_MAXLEN)
    @Getter @Setter private String owner;

    @NotNull @ManyToOne(optional=false)
    @Getter @Setter private FormField field;

    @Valid @Embedded
    @JsonIgnore @Getter @Setter private FormData data = new FormData();

    @Transient
    public String getValue () { return data == null ? null : data.getValue(); }
    public void setValue (String value) { data.setValue(value); }

    @Transient
    public String getDescription () { return data == null ? null : data.getDescription(); }
    public void setDescription (String description) { data.setDescription(description); }

}
