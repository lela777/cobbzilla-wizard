package org.cobbzilla.wizard.form.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.ChildEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;

@Entity
public class FormFieldMembership extends ChildEntity<FormFieldMembership, Form> {

    @ManyToOne(optional=false) @NotNull @Getter @Setter private FormField field;
    @ManyToOne(optional=false) @NotNull @Getter @Setter private Form form;

    @Size(max=FIELD_MEMBERSHIP_PLACEMENT_MAXLEN, message=ERR_FIELD_MEMBERSHIP_PLACEMENT_LENGTH)
    @Column(length=FIELD_MEMBERSHIP_PLACEMENT_MAXLEN)
    @Getter @Setter private String placement;

    @Override public Form getParent() { return getForm();  }
    @Override public void setParent(Form parent) { setForm(parent); }

}
