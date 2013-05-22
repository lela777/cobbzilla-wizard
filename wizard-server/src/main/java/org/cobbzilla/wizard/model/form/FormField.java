package org.cobbzilla.wizard.model.form;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static org.cobbzilla.wizard.model.form.FormConstraintConstants.*;

@Entity
public class FormField extends IdentifiableBase implements Identifiable {

    @HasValue(message=ERR_FIELD_NAME_EMPTY)
    @Size(max=MRFIELD_NAME_MAXLEN, message=ERR_FIELD_NAME_LENGTH)
    @JsonIgnore
    @Column(nullable = false, length=MRFIELD_NAME_MAXLEN)
    @Getter @Setter private String nameKey;

    @Transient
    public String getName () { return getNameKey(); }
    public void setName(String name) { setNameKey(name); }

    @Getter @Setter private String placement;

    @HasValue(message= ERR_FIELD_TYPE_EMPTY)
    @Size(max=MRFIELD_TYPE_MAXLEN, message=ERR_FIELD_TYPE_LENGTH)
    @Column(nullable = false, length=MRFIELD_TYPE_MAXLEN)
    @Getter @Setter
    private String fieldType;

    @NotNull(message=ERR_HAS_DESCRIPTION_EMPTY)
    @Column(nullable=false)
    @Getter @Setter private int hasDescription;

    @Transient @JsonIgnore
    public FormFieldType getFieldTypeEnum () { return FormFieldType.valueOf(fieldType); }

}
