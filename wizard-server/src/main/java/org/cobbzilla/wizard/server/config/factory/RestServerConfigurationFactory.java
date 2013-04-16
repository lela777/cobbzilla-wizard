package org.cobbzilla.wizard.server.config.factory;

import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.util.yml.YmlMerger;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RestServerConfigurationFactory<C extends RestServerConfiguration> {

    private final Yaml yaml = new Yaml();

    private final Class<C> configurationClass;

    public RestServerConfigurationFactory(Class<C> configurationClass) {
        this.configurationClass = configurationClass;
    }

    public C build(List<? extends ConfigurationSource> configurations) throws IOException {
        return build(configurations, null);
    }

    public C build(List<? extends ConfigurationSource> configurations, Map<String, String> env) throws IOException {

        YmlMerger ymlMerger = new YmlMerger(env);

        List<String> configFiles = new ArrayList<>(configurations.size());
        for (ConfigurationSource source : configurations) {
            configFiles.add(source.getFile().getAbsolutePath());
        }

        return yaml.loadAs(ymlMerger.mergeToString(configFiles), configurationClass);
    }

}
