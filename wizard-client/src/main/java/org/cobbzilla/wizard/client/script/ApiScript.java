package org.cobbzilla.wizard.client.script;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.ellipsis;
import static org.cobbzilla.util.time.TimeUtil.parseDuration;

public class ApiScript {

    @Getter @Setter private String comment;
    public boolean hasComment () { return !empty(comment); }

    @Getter @Setter private String include;
    public boolean hasInclude () { return !empty(include); }

    @Getter @Setter private Map<String, Object> params;
    public boolean hasParams () { return !empty(params); }

    @Getter @Setter private char paramStartDelim = '<';
    @Getter @Setter private char paramEndDelim = '>';

    @Getter @Setter private String delay;
    public boolean hasDelay () { return !empty(delay); }
    public long getDelayMillis () { return parseDuration(delay); }

    @Getter @Setter private String before;
    public boolean hasBefore () { return !empty(before); }

    @Getter @Setter private String after;
    public boolean hasAfter () { return !empty(after); }

    @Getter @Setter private String timeout;
    public long getTimeoutMillis () { return parseDuration(timeout); }

    @Getter @Setter private ApiScriptRequest request;
    @Getter @Setter private ApiScriptResponse response;
    public boolean hasResponse () { return response != null; }
    @JsonIgnore public String getRequestLine () { return request.getMethod() + " " + request.getUri(); }

    @Getter @Setter private long start = now();
    public long getAge () { return now() - start; }

    public boolean isTimedOut() { return getAge() > getTimeoutMillis(); }

    @Override public String toString() {
        return "{\n" +
                "  \"comment\": \"" + comment + "\"," +
                "  \"request\": \"" + ellipsis(json(request), 300) + "\",\n" +
                "  \"response\": \"" + ellipsis(json(response), 300) + "\",\n" +
                "}";
    }
}
