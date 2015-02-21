package org.cobbzilla.wizard.util;

import com.sun.jersey.api.core.HttpContext;

import javax.ws.rs.core.MultivaluedMap;

public class HttpContextUtil {

    public static String getQueryParams(HttpContext context) {

        final StringBuilder b = new StringBuilder();
        final MultivaluedMap<String, String> params = context.getRequest().getQueryParameters();

        if (params != null && !params.isEmpty()) {
            for (String name : params.keySet()) {
                for (String value : params.get(name)) {
                    if (b.length() > 0) b.append("&");
                    if (b.length() == 0) b.append("?");
                    b.append(name).append("=").append(value);
                }
            }
        }

        return b.toString();
    }

}
