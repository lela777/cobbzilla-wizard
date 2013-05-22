package org.cobbzilla.wizard.model.form;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Entity;

@Entity
public class Form extends IdentifiableBase implements Identifiable {

    @Getter @Setter private String nameMsgKey;
    @Getter @Setter private String formType;
    @Getter @Setter private String owner;

}
