package org.cobbzilla.wizard.dao.form;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.form.FormGroupMembership;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormGroupMembershipDAO extends AbstractCRUDDAO<FormGroupMembership> {

    public List<FormGroupMembership> findByForm(String uuid) {
        return list(criteria().createCriteria("form").add(Restrictions.eq("uuid", uuid)));
    }

}
