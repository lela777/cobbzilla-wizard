package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.dao.AbstractChildCRUDDAO;
import org.cobbzilla.wizard.form.model.Form;
import org.cobbzilla.wizard.form.model.FormGroupMembership;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormGroupMembershipDAO extends AbstractChildCRUDDAO<FormGroupMembership, Form> {

    public FormGroupMembershipDAO() { super(Form.class); }

    public List<FormGroupMembership> findByForm(String uuid) {
        return list(criteria().createCriteria("form").add(Restrictions.eq("uuid", uuid)));
    }

}
