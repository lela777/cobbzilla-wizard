package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.dao.AbstractChildCRUDDAO;
import org.cobbzilla.wizard.form.model.FormFieldGroup;
import org.cobbzilla.wizard.form.model.FormFieldGroupMembership;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormFieldGroupMembershipDAO extends AbstractChildCRUDDAO<FormFieldGroupMembership, FormFieldGroup> {

    public FormFieldGroupMembershipDAO() { super(FormFieldGroup.class); }

    public List<FormFieldGroupMembership> findByGroup (String uuid) {
        return list(criteria().createCriteria("fieldGroup").add(Restrictions.eq("uuid", uuid)));
    }

}
