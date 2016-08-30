package org.cobbzilla.wizard.server.config;

import airbrake.AirbrakeNoticeBuilder;
import airbrake.AirbrakeNotifier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandShell;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Slf4j
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
        final int responseCode = getNotifier().notify(builder.newNotice());
        if (responseCode != 200) log.warn("report("+e+"): notifier API returned "+responseCode);
    }

}
