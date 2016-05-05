package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.cobbzilla.util.http.HttpMethods;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.json.JsonUtil.mergeJsonOrDie;

@ToString
public class ApiScriptRequest {

    @Getter @Setter private String uri;
    @Getter @Setter private String data;
    @Getter @Setter private JsonNode entity;

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

}
