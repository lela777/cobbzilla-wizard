package org.cobbzilla.wizard.form.dao;

import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.form.model.FormFieldValue;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class FormFieldValueDAO extends AbstractCRUDDAO<FormFieldValue> {

    public List<FormFieldValue> findByOwner(String ownerUuid) {
        return list(criteria().add(Restrictions.eq("owner", ownerUuid)));
    }

    public Map<String, FormFieldValue> mapFieldsByOwner(String ownerUuid) {
        List<FormFieldValue> values = findByOwner(ownerUuid);
        Map<String, FormFieldValue> map = new HashMap<>(values.size());
        for (FormFieldValue value : values) {
            map.put(value.getField().getUuid(), value);
        }
        return map;
    }

}
