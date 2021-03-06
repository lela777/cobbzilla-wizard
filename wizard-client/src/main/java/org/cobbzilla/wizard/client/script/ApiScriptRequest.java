package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.http.HttpMethods;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.json.JsonUtil.mergeJsonOrDie;
import static org.cobbzilla.util.string.StringUtil.ellipsis;

public class ApiScriptRequest {

    @Getter @Setter private String uri;
    @Getter @Setter private String data;
    public boolean hasData () { return !empty(data); }

    @Getter @Setter private boolean handlebarsEnabled = true;

    @Getter @Setter private JsonNode entity;
    public boolean hasEntity () { return !empty(entity); }

    @Getter @Setter private String session;
    public boolean hasSession () { return !empty(session); }

    @Setter private String method;

    public String getMethod() {
        if (!empty(method)) return method;
        if (entity != null) return HttpMethods.POST;
        return HttpMethods.GET;
    }

    public String getJsonEntity(Map<String, Object> ctx) {
        if (data == null) return json(entity);
        final Object o = ctx.get(data);
        if (o == null) die("getJsonEntity: data '" + data + "' not found in context: " + ctx);
        return mergeJsonOrDie(json(o), entity);
    }

    @Override public String toString () {
        return "{uri: "+uri
                + ", method: "+getMethod()
                + (hasData() ? ", data: "+ ellipsis(data, 1000) : "")
                + (hasEntity() ? ", entity: " + ellipsis(json(entity), 1000) : "")
                + (hasSession() ? ", session: " + getSession() : "")
                +"}";
    }

}
