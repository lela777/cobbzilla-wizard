package org.cobbzilla.wizard.dao.form;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.form.FormFieldGroupMembership;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormFieldGroupMembershipDAO extends AbstractCRUDDAO<FormFieldGroupMembership> {

    public List<FormFieldGroupMembership> findByGroup (String uuid) {
        return list(criteria().createCriteria("fieldGroup").add(Restrictions.eq("uuid", uuid)));
    }

}
