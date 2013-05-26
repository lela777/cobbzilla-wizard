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
public class FormGroupMembership extends ChildEntity<FormGroupMembership, Form> implements Identifiable {

    @ManyToOne(optional=false) @NotNull @Getter @Setter private Form form;
    @ManyToOne(optional=false) @NotNull @Getter @Setter private FormFieldGroup fieldGroup;
    @Getter @Setter private Integer placement;

    @Override @Transient public Form getParent() { return getForm(); }
    @Override public void setParent(Form parent) { setForm(parent); }

}
