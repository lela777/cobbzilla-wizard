package org.cobbzilla.wizard.server.config.factory;

import java.io.File;
import java.io.IOException;

public interface ConfigurationSource {

    File getFile() throws IOException;

}
