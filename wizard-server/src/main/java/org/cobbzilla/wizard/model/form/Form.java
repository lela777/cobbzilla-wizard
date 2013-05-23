package org.cobbzilla.wizard.model.form;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.cobbzilla.wizard.validation.HasValue;
import org.cobbzilla.wizard.validation.IsUnique;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.form.FormConstraintConstants.*;

@Entity
@IsUnique(unique="nameKey", message=ERR_FORM_NAME_UNIQUE, daoBean="formDAO")
public class Form extends IdentifiableBase implements Identifiable {

    @HasValue(message=ERR_FORM_NAME_EMPTY)
    @Size(max= FORM_NAME_MAXLEN, message=ERR_FORM_NAME_LENGTH)
    @Column(unique=true, nullable=false, length=FORM_NAME_MAXLEN)
    @Getter @Setter private String nameKey;

    @HasValue(message=ERR_FORM_TYPE_EMPTY)
    @Size(max= FORM_TYPE_MAXLEN, message=ERR_FORM_TYPE_LENGTH)
    @Column(nullable = false, length=FORM_TYPE_MAXLEN)
    @Getter @Setter private String formType;

    @Size(max=FORM_OWNER_MAXLEN, message=ERR_FORM_OWNER_LENGTH)
    @Column(length=FORM_OWNER_MAXLEN)
    @Getter @Setter private String owner;

}
