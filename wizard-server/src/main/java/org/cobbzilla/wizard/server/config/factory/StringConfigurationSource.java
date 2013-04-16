package org.cobbzilla.wizard.server.config.factory;

import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.StreamUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

@AllArgsConstructor
public class StringConfigurationSource implements ConfigurationSource {

    private String value;

    @Override
    public File getFile() throws IOException {
        return StreamUtil.stream2file(new ByteArrayInputStream(value.getBytes()));
    }

}
