package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.model.FormFieldMembership;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FormFieldMembershipDAO extends AbstractCRUDDAO<FormFieldMembership> {

    public List<FormFieldMembership> findByFormUuid(String formUuid) {
        return list(criteria().addOrder(Order.asc("placement")).createCriteria("form").add(Restrictions.eq("uuid", formUuid)));
    }

    public List<FormFieldMembership> findByOwner(String owner) {
        return list(criteria().createCriteria("form").add(Restrictions.eq("owner", owner)));
    }
}
