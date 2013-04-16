package org.cobbzilla.wizard.server.config.factory;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;

@AllArgsConstructor
public class FileConfigurationSource implements ConfigurationSource {

    @Getter private File file;

}
