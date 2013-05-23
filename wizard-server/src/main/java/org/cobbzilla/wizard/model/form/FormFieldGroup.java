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
@IsUnique(unique="nameKey", message=ERR_FIELD_GROUP_NAME_UNIQUE, daoBean="formFieldGroupDAO")
public class FormFieldGroup extends IdentifiableBase implements Identifiable {

    @HasValue(message=ERR_FIELD_GROUP_NAME_EMPTY)
    @Size(max=FIELD_GROUP_NAME_MAXLEN, message=ERR_FIELD_GROUP_NAME_LENGTH)
    @Column(unique=true, nullable=false, length=FIELD_NAME_MAXLEN)
    @Getter @Setter private String nameKey;

    @Size(max=FORM_OWNER_MAXLEN, message=ERR_FORM_OWNER_LENGTH)
    @Column(length=FORM_OWNER_MAXLEN)
    @Getter @Setter private String owner;

    @HasValue(message=ERR_FIELD_GROUP_TYPE_EMPTY)
    @Size(max= FIELD_GROUP_TYPE_MAXLEN, message=ERR_FIELD_GROUP_TYPE_LENGTH)
    @Column(nullable=false, length= FIELD_TYPE_MAXLEN)
    @Getter @Setter private String fieldGroupType;

}
