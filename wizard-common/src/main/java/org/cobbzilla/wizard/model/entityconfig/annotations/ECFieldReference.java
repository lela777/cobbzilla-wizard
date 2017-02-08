package org.cobbzilla.wizard.model.entityconfig.annotations;

import org.cobbzilla.wizard.model.entityconfig.EntityFieldReference;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD)
public @interface ECFieldReference {
    String control() default "hidden";
    String refEntity() default EntityFieldReference.REF_PARENT;
    String refField() default "uuid";
    String refDisplayField() default "name";
}
