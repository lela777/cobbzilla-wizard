package org.cobbzilla.wizard.model.form;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

@Entity
public class FormGroupMembership extends IdentifiableBase implements Identifiable {

    @ManyToOne(optional=false) @NotNull @Getter @Setter private Form form;
    @ManyToOne(optional=false) @NotNull @Getter @Setter private FormFieldGroup fieldGroup;
    @Getter @Setter private Integer placement;

}
