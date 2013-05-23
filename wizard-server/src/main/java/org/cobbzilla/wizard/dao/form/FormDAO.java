package org.cobbzilla.wizard.dao.form;

import org.cobbzilla.wizard.model.form.Form;
import org.springframework.stereotype.Repository;

@Repository
public class FormDAO extends FormNameKeyUniqueValidatorDAO<Form> {

    public Form findByNameKey(String uniqueValue) { return findByUniqueField("nameKey", uniqueValue); }

}
