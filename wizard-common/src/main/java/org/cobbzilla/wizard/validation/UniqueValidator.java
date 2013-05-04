package org.cobbzilla.wizard.validation;

import org.cobbzilla.util.reflect.ReflectionUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.ws.rs.ext.Provider;

@Provider
@Service
public class UniqueValidator implements ConstraintValidator<IsUnique, Object>, ApplicationContextAware {

    private String uniqueFieldName;
    private String idFieldName;
    private String daoBean;

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        UniqueValidator.applicationContext = applicationContext;
    }

    @Override
    public void initialize(IsUnique constraintAnnotation) {
        this.uniqueFieldName = constraintAnnotation.uniqueFieldName();
        this.idFieldName = constraintAnnotation.idFieldName();
        this.daoBean = constraintAnnotation.daoBean();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {

        UniqueValidatorDao dao = (UniqueValidatorDao) applicationContext.getBean(daoBean);

        Object idValue = (idFieldName.equals(IsUnique.CREATE_ONLY) || !ReflectionUtil.hasGetter(object, idFieldName)) ? null : ReflectionUtil.get(object, idFieldName);
        Object fieldValue = ReflectionUtil.get(object, uniqueFieldName);
        if (idValue == null) {
            return dao.isUnique(uniqueFieldName, fieldValue);
        }
        return dao.isUnique(uniqueFieldName, fieldValue, idFieldName, idValue);
    }

}
