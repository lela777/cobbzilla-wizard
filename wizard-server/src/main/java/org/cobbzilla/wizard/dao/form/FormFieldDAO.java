package org.cobbzilla.wizard.dao.form;

import org.cobbzilla.wizard.model.form.FormField;
import org.springframework.stereotype.Repository;

@Repository
public class FormFieldDAO extends FormNameKeyUniqueValidatorDAO<FormField> {

    public FormField findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
