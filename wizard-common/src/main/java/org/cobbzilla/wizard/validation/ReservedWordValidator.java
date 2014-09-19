package org.cobbzilla.wizard.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReservedWordValidator implements ConstraintValidator<NotReservedWord, Object> {

    private Class<? extends ReservedWords> reserved;

    @Override
    public void initialize(NotReservedWord constraintAnnotation) {
        reserved = constraintAnnotation.reserved();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return true;
        final String[] words = getReservedWords();
        for (String word : words) {
            if (value.toString().equalsIgnoreCase(word)) return false;
        }
        return true;
    }

    private Map<Class, String[]> cache = new ConcurrentHashMap<>();

    private String[] getReservedWords() {
        String[] words = cache.get(reserved);
        if (words == null) {
            try {
                words = reserved.newInstance().getReservedWords();
            } catch (Exception e) {
                throw new IllegalStateException("Error instantiating ReservedWords class ("+reserved.getName()+"): "+e, e);
            }
            cache.put(reserved, words);
        }
        return words;
    }

}
