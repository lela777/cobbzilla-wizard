package org.cobbzilla.wizard.form.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.cobbzilla.wizard.validation.HasValue;
import org.cobbzilla.wizard.validation.IsUnique;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.form.model.FormConstraintConstants.*;

@Entity
@IsUnique(unique="nameKey", message=ERR_FIELD_NAME_UNIQUE, daoBean="formFieldDAO")
public class FormField extends IdentifiableBase implements Identifiable {

    @HasValue(message=ERR_FIELD_NAME_EMPTY)
    @Size(max=FIELD_NAME_MAXLEN, message=ERR_FIELD_NAME_LENGTH)
    @Column(unique=true, nullable=false, length=FIELD_NAME_MAXLEN)
    @Getter @Setter private String nameKey;

    @Transient
    public String getName () { return getNameKey(); }
    public void setName(String name) { setNameKey(name); }

    @HasValue(message=ERR_FIELD_DEFAULT_NAME_EMPTY)
    @Size(max=FIELD_DEFAULT_NAME_MAXLEN, message=ERR_FIELD_DEFAULT_NAME_LENGTH)
    @Column(unique=true, nullable=false, length=FIELD_DEFAULT_NAME_MAXLEN)
    @Getter @Setter private String defaultName;

    @Size(max=FORM_OWNER_MAXLEN, message=ERR_FORM_OWNER_LENGTH)
    @Column(length=FORM_OWNER_MAXLEN)
    @Getter @Setter private String owner;

    @HasValue(message=ERR_FIELD_TYPE_EMPTY)
    @Size(max=FIELD_TYPE_MAXLEN, message=ERR_FIELD_TYPE_LENGTH)
    @Column(nullable=false, length=FIELD_TYPE_MAXLEN)
    @Getter @Setter private String fieldType;

    @Size(max=FIELD_OPTS_TYPE_MAXLEN, message=ERR_FIELD_OPTS_TYPE_LENGTH)
    @Column(length=FIELD_OPTS_TYPE_MAXLEN)
    @Getter @Setter private String fieldOptions;

    @NotNull(message=ERR_HAS_DESCRIPTION_EMPTY)
    @Column(nullable=false)
    @Getter @Setter private int hasDescription;

    @Transient @JsonIgnore
    public FormFieldType getFieldTypeEnum () { return FormFieldType.valueOf(fieldType); }

}
