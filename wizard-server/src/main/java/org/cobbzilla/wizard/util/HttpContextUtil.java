package org.cobbzilla.wizard.util;

import com.sun.jersey.api.core.HttpContext;

import javax.ws.rs.core.MultivaluedMap;

public class HttpContextUtil {

    public static String getQueryParams(HttpContext context) {
        final MultivaluedMap<String, String> params = context.getRequest().getQueryParameters();
        return (params == null || params.isEmpty()) ? "" : "?" + encodeParams(params);
    }

    public static String encodeParams(MultivaluedMap<String, String> params) {
        final StringBuilder b = new StringBuilder();
        if (params != null && !params.isEmpty()) {
            for (String name : params.keySet()) {
                for (String value : params.get(name)) {
                    if (b.length() > 0) b.append("&");
                    b.append(name).append("=").append(value);
                }
            }
        }
        return b.toString();
    }

}
