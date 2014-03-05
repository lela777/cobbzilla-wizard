package org.cobbzilla.wizard.form.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.cobbzilla.wizard.validation.HasValue;
import org.cobbzilla.wizard.validation.IsUnique;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.List;

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

    public static final String OPTIONS_SEPARATOR = "|";
    public static final List<String> EMPTY_OPTIONS = Collections.emptyList();

    @Transient public List<String> getOptions () {
        return StringUtil.empty(fieldOptions) ? EMPTY_OPTIONS : StringUtil.split(fieldOptions, OPTIONS_SEPARATOR);
    }

    public void setOptions(List<String> options) {
        fieldOptions = (options == null) ? null : StringUtils.join(options, OPTIONS_SEPARATOR);
    }
    public void setOptionsArray(String[] options) {
        fieldOptions = (options == null) ? null : StringUtils.join(options, OPTIONS_SEPARATOR);
    }

    public boolean hasOptions () { return !StringUtil.empty(fieldOptions); }


    @NotNull(message=ERR_HAS_DESCRIPTION_EMPTY)
    @Column(nullable=false)
    @Getter @Setter private boolean hasDescription = false;

    @Transient @JsonIgnore
    public FormFieldType getFieldTypeEnum () { return FormFieldType.valueOf(fieldType); }

}
