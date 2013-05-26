package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.form.model.FormFieldGroup;
import org.springframework.stereotype.Repository;

@Repository
public class FormFieldGroupDAO extends FormNameKeyUniqueValidatorDAO<FormFieldGroup> {

    public FormFieldGroup findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
