package org.cobbzilla.wizard.client.script;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ApiScript {

    @Getter @Setter private String comment;
    @Getter @Setter private ApiScriptRequest request;
    @Getter @Setter private ApiScriptResponse response;
    public boolean hasResponse () { return response != null; }

}
