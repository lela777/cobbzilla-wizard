package org.cobbzilla.wizard.server.config.factory;

import lombok.AllArgsConstructor;
import org.cobbzilla.util.io.StreamUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class StreamConfigurationSource implements ConfigurationSource {

    private InputStream stream;

    @Override
    public File getFile() throws IOException { return StreamUtil.stream2file(stream); }

    public static List<ConfigurationSource> fromResources (Class clazz, String... streams) {
        List<ConfigurationSource> list = new ArrayList<>(streams.length);
        for (String stream : streams) {
            list.add(new StreamConfigurationSource(clazz.getResourceAsStream(stream)));
        }
        return list;
    }
}
