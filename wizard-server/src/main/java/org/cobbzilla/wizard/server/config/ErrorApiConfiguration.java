package org.cobbzilla.wizard.server.config;

import airbrake.AirbrakeNoticeBuilder;
import airbrake.AirbrakeNotifier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cobbzilla.util.system.CommandShell;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor
public class ErrorApiConfiguration {

    @Getter @Setter private String url;
    @Getter @Setter private String key;
    @Setter private String env;

    @Getter(lazy=true) private final AirbrakeNotifier notifier = initNotifier();
    private AirbrakeNotifier initNotifier() { return new AirbrakeNotifier(getUrl()); }

    public String getEnv() { return !empty(env) ? env : CommandShell.hostname(); }

    public boolean isValid() { return !empty(getUrl()) && !empty(getKey()) && !empty(getEnv()); }

    public void report(Exception e) {
        final AirbrakeNoticeBuilder builder = new AirbrakeNoticeBuilder(getKey(), e, getEnv());
        getNotifier().notify(builder.newNotice());
    }
}
