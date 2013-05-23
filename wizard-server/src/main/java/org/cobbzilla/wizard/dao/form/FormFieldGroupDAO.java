package org.cobbzilla.wizard.dao.form;

import org.cobbzilla.wizard.model.form.FormFieldGroup;
import org.springframework.stereotype.Repository;

@Repository
public class FormFieldGroupDAO extends FormNameKeyUniqueValidatorDAO<FormFieldGroup> {

    public FormFieldGroup findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
