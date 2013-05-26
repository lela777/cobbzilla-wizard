package org.cobbzilla.wizard.form.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.ChildEntity;
import org.cobbzilla.wizard.model.Identifiable;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Entity
public class FormFieldGroupMembership extends ChildEntity<FormFieldGroupMembership, FormFieldGroup> implements Identifiable {

    @ManyToOne(optional=false) @NotNull @Getter @Setter private FormFieldGroup fieldGroup;
    @ManyToOne(optional=false) @NotNull @Getter @Setter private FormField field;
    @Getter @Setter private Integer placement;

    @Override @Transient public FormFieldGroup getParent() { return getFieldGroup(); }
    @Override public void setParent(FormFieldGroup parent) { setFieldGroup(parent); }

}
