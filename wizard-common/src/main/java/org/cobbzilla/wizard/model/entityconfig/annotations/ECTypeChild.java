package org.cobbzilla.wizard.model.entityconfig.annotations;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) @Repeatable(value=ECTypeChildren.class)
public @interface ECTypeChild {
    String className();
    String name() default "";
    String backref();
    ECFieldReference parentFieldRef() default @ECFieldReference();
}
