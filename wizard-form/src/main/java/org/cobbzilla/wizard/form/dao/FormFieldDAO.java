package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.form.model.FormField;
import org.springframework.stereotype.Repository;

@Repository public class FormFieldDAO extends FormNameKeyUniqueValidatorDAO<FormField> {

    public FormField findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
