package org.cobbzilla.wizard.log;

import ch.qos.logback.core.OutputStreamAppender;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

import java.io.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public class LogRelayAppender<E> extends OutputStreamAppender<E> {

    @Getter @Setter private static volatile RestServerConfiguration config;
    @Getter private PipedInputStream in = new PipedInputStream();

    public static final long STARTUP_TIMEOUT = SECONDS.toMillis(30);

    private void superStart() { super.start(); }

    @Override public void start() {
        final String simpleClass = getClass().getSimpleName();
        try {
            setOutputStream(new PipedOutputStream(in));
            daemon(() -> {
                final long start = now();
                while ((config == null || config.getApplicationContext() == null) && (now() - start < STARTUP_TIMEOUT)) {
                    sleep(SECONDS.toMillis(1));
                }
                if (config == null) {
                    log.warn(simpleClass+": RestServerConfiguration was never set, exiting");
                    return;
                }
                if (config.getApplicationContext() == null) {
                    log.warn(simpleClass+": RestServerConfiguration.applicationContext was never set, exiting");
                    return;
                }
                final BufferedReader reader;
                try {
                    reader = new BufferedReader(new InputStreamReader(in));
                } catch (Exception e) {
                    log.warn(simpleClass+": error setting up reader, exiting");
                    return;
                }
                final LogRelayAppenderConfig logRelayConfig = config.getLogRelay();
                if (logRelayConfig == null) {
                    log.warn(simpleClass+": no relayConfig was found, exiting");
                    return;
                }
                final String relayTo = logRelayConfig.getRelayTo();
                if (empty(relayTo)) {
                    log.warn(simpleClass+": relayConfig was found, but relayTo was empty, exiting");
                    return;
                }
                final LogRelayAppenderTarget relayTarget;
                try {
                    // cast shouldn't be required, but a compilation error occurs if we remove it
                    relayTarget = (LogRelayAppenderTarget) config.getBean(relayTo);
                    relayTarget.init(logRelayConfig.getParams());
                } catch (Exception e) {
                    log.warn(simpleClass+": error initializing relayTo ("+ relayTo +"), exiting: "+e);
                    return;
                }

                superStart();
                String line = null;
                try {
                    while ((line = reader.readLine()) != null) relayTarget.relay(line);
                } catch (Exception e) {
                    throw new IllegalStateException("LogRelayAppender: error relaying line ("+line+"), exiting: "+e);
                }
            });
        } catch (IOException e) {
            die("start: error setting up pipe: "+e, e);
        }
    }

}
