package org.cobbzilla.wizard.form.resources;

import org.cobbzilla.wizard.form.server.FormApiConfiguration;
import org.cobbzilla.wizard.form.server.FormApiServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;

import java.util.List;

public class FormResourceITBase extends ApiDocsResourceIT<FormApiConfiguration, FormApiServer> {

    @Override protected List<ConfigurationSource> getConfigurations() {
        return getConfigurationSources("/conf/form-api-test.yml");
    }

}
