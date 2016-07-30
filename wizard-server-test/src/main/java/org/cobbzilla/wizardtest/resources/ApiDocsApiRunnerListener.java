package org.cobbzilla.wizardtest.resources;

import lombok.AllArgsConstructor;
import org.cobbzilla.restex.targets.TemplateCaptureTarget;
import org.cobbzilla.wizard.client.script.ApiRunnerListenerBase;
import org.cobbzilla.wizard.client.script.ApiScript;

import java.util.Map;

@AllArgsConstructor
public class ApiDocsApiRunnerListener extends ApiRunnerListenerBase {

    private TemplateCaptureTarget apiDocs;

    @Override public void beforeCall(ApiScript script, Map<String, Object> ctx) {
        apiDocs.addNote(script.getComment());
    }

}
