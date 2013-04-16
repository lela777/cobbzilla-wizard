package org.cobbzilla.wizard.validation;

import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.resources.EntityMappableResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.ext.Provider;
import java.util.List;

@Provider
@Service
public class UniqueValidator implements ConstraintValidator<IsUnique, Object> {

    private String id;
    private String field;
    private String persist;

    @Override
    public void initialize(IsUnique constraintAnnotation) {
        this.id = constraintAnnotation.id();
        this.field = constraintAnnotation.field();
        this.persist = constraintAnnotation.persist();
        if (this.persist.equals(IsUnique.SAME_AS_FIELD)) this.persist = this.field;
    }

    private static final String PARAM_FIELD = "field";
    private static final String PARAM_ID = "id";
    private static final String[] PARAM_FIELD_ARRAY = {PARAM_FIELD};
    private static final String[] PARAM_NAMES = {PARAM_FIELD, PARAM_ID};

    protected static HibernateTemplate hibernateTemplate;

    @Autowired
    private void setHibernateTemplate (HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {

        final Class<?> entityClass;
            if (object instanceof EntityMappableResource) {
            entityClass = ((EntityMappableResource) object).getEntityClass();
        } else {
            entityClass = object.getClass();
        }

        Object idValue = (id.equals(IsUnique.CREATE_ONLY) || !ReflectionUtil.hasGetter(object, id)) ? null : ReflectionUtil.get(object, id);
        Object fieldValue = ReflectionUtil.get(object, field);

        final String[] params = (idValue == null) ? PARAM_FIELD_ARRAY : PARAM_NAMES;
        final Object[] values = (idValue == null) ? new Object[] { fieldValue } : new Object[] { fieldValue, idValue };

        final StringBuilder queryString
                = new StringBuilder().append("from ").append(entityClass.getSimpleName())
                .append(" x where x.").append(persist).append("=:").append(PARAM_FIELD);
        if (idValue != null) queryString.append(" and x.id != :").append(PARAM_ID);

        final List found = hibernateTemplate.findByNamedParam(queryString.toString(), params, values);
        return (found == null || found.size() == 0);
    }

}
