package org.cobbzilla.wizard.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = HasValueValidator.class)
@Documented
public @interface HasValue {

    public abstract String message() default "{org.cobbzilla.validator.constraints.HasValue.message}";

    public abstract Class<?>[] groups() default { };

    public abstract Class<? extends Payload>[] payload() default { };

    /**
     * Defines several {@code @NotBlank} annotations on the same element.
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER })
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        HasValue[] value();
    }

}
