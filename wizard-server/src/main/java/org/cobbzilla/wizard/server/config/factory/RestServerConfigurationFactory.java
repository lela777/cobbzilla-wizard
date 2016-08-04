package org.cobbzilla.wizard.server.config.factory;

import org.cobbzilla.util.yml.YmlMerger;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;

public class RestServerConfigurationFactory<C extends RestServerConfiguration> {

    private final Yaml yaml = new Yaml();

    private final Class<C> configurationClass;

    public RestServerConfigurationFactory(Class<C> configurationClass) { this.configurationClass = configurationClass; }

    public C build(List<? extends ConfigurationSource> configurations) throws IOException {
        return build(configurations, null);
    }

    public C build(List<? extends ConfigurationSource> configurations, Map<String, String> env) {

        final YmlMerger ymlMerger = new YmlMerger(env);

        final List<String> configFiles = new ArrayList<>(configurations.size());
        try {
            for (ConfigurationSource source : configurations) {
                configFiles.add(abs(source.getFile()));
            }

            final String yaml = ymlMerger.mergeToString(configFiles);
            return this.yaml.loadAs(yaml, configurationClass);

        } catch (Exception e) {
            return die("build: "+e, e);
        }
    }

}
