package org.cobbzilla.wizard.resources;

import javax.ws.rs.core.Response;
import java.util.Collections;

public class ResourceUtil {

    public static Response notFound(String id) {
        if (id == null) id = "-unknown-";
        return Response.status(Response.Status.NOT_FOUND).entity(Collections.singletonMap("resource", id)).build();
    }

    public static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
}
