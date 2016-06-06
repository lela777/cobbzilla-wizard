package org.cobbzilla.wizard.resources;

import com.sun.jersey.api.core.HttpContext;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.model.NamedIdentityBase;

import static org.cobbzilla.wizard.resources.ResourceUtil.optionalUserPrincipal;

public abstract class NamedSystemResource<E extends NamedIdentityBase> extends NamedResource<E> {

    protected boolean isAdmin(HttpContext ctx) {
        final Object thing = optionalUserPrincipal(ctx);
        if (thing == null) return false;
        final Object admin = ReflectionUtil.get(thing, "admin");
        return admin != null && Boolean.valueOf(admin.toString());
    }

    @Override protected boolean canCreate(HttpContext ctx) { return isAdmin(ctx); }
    @Override protected boolean canUpdate(HttpContext ctx) { return isAdmin(ctx); }
    @Override protected boolean canDelete(HttpContext ctx) { return isAdmin(ctx); }

}
