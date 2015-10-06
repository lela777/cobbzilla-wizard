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

    public StreamConfigurationSource (String resourcePath) {
        this(StreamConfigurationSource.class.getClassLoader().getResourceAsStream(resourcePath));
    }

    private InputStream stream;

    @Override public File getFile() throws IOException { return StreamUtil.stream2temp(stream); }

    public static List<ConfigurationSource> fromResources (Class clazz, String... streams) {
        List<ConfigurationSource> list = new ArrayList<>(streams.length);
        for (String stream : streams) {
            InputStream in = clazz.getResourceAsStream(stream);
            if (in == null) in = clazz.getClassLoader().getResourceAsStream(stream);
            if (in == null) throw new IllegalArgumentException("StreamConfigurationSource.fromResources: Couldn't find stream: "+stream);
            list.add(new StreamConfigurationSource(in));
        }
        return list;
    }
}
