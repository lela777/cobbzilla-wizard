package org.cobbzilla.wizard.model.entityconfig.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface ECTypeChildren {
    ECTypeChild[] value();
}
