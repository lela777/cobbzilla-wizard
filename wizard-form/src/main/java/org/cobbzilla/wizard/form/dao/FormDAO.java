package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.form.model.Form;
import org.springframework.stereotype.Repository;

@Repository public class FormDAO extends FormNameKeyUniqueValidatorDAO<Form> {

    public Form findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
