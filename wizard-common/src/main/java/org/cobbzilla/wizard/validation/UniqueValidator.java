package org.cobbzilla.wizard.validation;

import org.cobbzilla.util.reflect.ReflectionUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.ext.Provider;

@Provider @Service
public class UniqueValidator implements ConstraintValidator<IsUnique, Object>, ApplicationContextAware {

    private String uniqueProperty;
    private String uniqueField;
    private String idProperty;
    private String idField;
    private String daoBean;

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        UniqueValidator.applicationContext = applicationContext;
    }

    @Override
    public void initialize(IsUnique constraintAnnotation) {
        this.uniqueProperty = constraintAnnotation.unique();
        this.uniqueField = constraintAnnotation.uniqueField();
        this.idProperty = constraintAnnotation.id();
        this.idField = constraintAnnotation.idField();
        this.daoBean = constraintAnnotation.daoBean();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {

        UniqueValidatorDao dao = (UniqueValidatorDao) applicationContext.getBean(daoBean);

        Object idValue = (idProperty.equals(IsUnique.CREATE_ONLY) || !ReflectionUtil.hasGetter(object, idProperty)) ? null : ReflectionUtil.get(object, idProperty);
        Object fieldValue = ReflectionUtil.get(object, uniqueProperty);

        if (uniqueField.equals(IsUnique.DEFAULT)) uniqueField = uniqueProperty;
        if (idField.equals(IsUnique.DEFAULT)) idField = idProperty;

        if (idValue == null) {
            return dao.isUnique(uniqueField, fieldValue);
        }
        return dao.isUnique(uniqueField, fieldValue, idField, idValue);
    }

}
