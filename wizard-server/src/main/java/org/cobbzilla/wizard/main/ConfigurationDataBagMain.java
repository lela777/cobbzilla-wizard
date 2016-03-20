package org.cobbzilla.wizard.main;

import lombok.AllArgsConstructor;
import org.cobbzilla.util.main.BaseMain;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerHarness;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;
import org.cobbzilla.wizard.server.config.factory.FileConfigurationSource;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.toFileOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

public class ConfigurationDataBagMain extends BaseMain<ConfigurationDataBagOptions> {

    public static void main (String[] args) { main(ConfigurationDataBagMain.class, args); }

    @Override protected void run() throws Exception {

        final ConfigurationDataBagOptions opts = getOptions();
        final String serverClass = opts.getServerClass();
        final File configFile = opts.getConfigFile();
        if (!configFile.exists()) die("config file does not exist: "+abs(configFile));

        final Class<? extends RestServer> clazz = forName(serverClass);
        final RestServerHarness harness = new RestServerHarness(clazz);
        harness.addConfiguration(new FileConfigurationSource(configFile));
        harness.init(opts.getEnv());
        harness.getServer().addLifecycleListener(new DatabagWriter(this));
        harness.startServer();

        synchronized (this) {
            this.wait();
        }
    }

    @AllArgsConstructor
    private class DatabagWriter extends RestServerLifecycleListenerBase {
        private final ConfigurationDataBagMain main;

        @Override public void onStart(RestServer server) {
            super.onStart(server);
            final String databagJson = toJsonOrDie(server.getConfiguration());
            if (main.getOptions().hasOutput()) {
                toFileOrDie(main.getOptions().getOutput(), databagJson);
            } else {
                out(databagJson);
            }
            synchronized (main) {
                main.notify();
            }
        }
    }
}
