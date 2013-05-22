package org.cobbzilla.wizard.model.form;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

@Entity
public class FormFieldGroupMembership extends IdentifiableBase implements Identifiable {

    @ManyToOne(optional=false) @Getter @Setter private FormFieldGroup fieldGroup;
    @ManyToOne(optional=false) @Getter @Setter private FormField field;
    @Getter @Setter private Integer placement;
}
