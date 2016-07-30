package org.cobbzilla.wizardtest.resources;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.client.script.ApiRunnerListenerBase;
import org.cobbzilla.wizard.client.script.ApiScript;

import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

@AllArgsConstructor
public class ApiDocsApiRunnerListener extends ApiRunnerListenerBase {

    private TemplateCaptureTarget apiDocs;

    @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {
        apiDocs.addNote(script.getComment());
    }

}
