package org.cobbzilla.wizard.log;

import ch.qos.logback.core.OutputStreamAppender;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.Bytes;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

import java.io.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.closeQuietly;
import static org.cobbzilla.util.system.Sleep.sleep;

@Slf4j
public class LogRelayAppender<E> extends OutputStreamAppender<E> {

    @Getter @Setter private static volatile RestServerConfiguration config;

    public static final long STARTUP_TIMEOUT = SECONDS.toMillis(30);
    private static final int PIPE_BUFSIZ = (int) (8*Bytes.MB);

    private PipedInputStream in;
    private PipedOutputStream out;

    @Override public void stop() {
        if (in != null) closeQuietly(in);
        if (out != null) closeQuietly(out);
        super.stop();
    }

    // allows lambda to call our superclass's start method
    private void superStart () { super.start(); }

    @Override public void start() {
        final String simpleClass = getClass().getSimpleName();
        try {
            in = new PipedInputStream(PIPE_BUFSIZ);
            out = new PipedOutputStream(in);
            setOutputStream(out);
        } catch (IOException e) {
            stop();
            throw new IllegalStateException("start: error setting up pipes: "+e, e);
        }
        daemon(() -> {
            final long start = now();

            // wait for config to get set (someone has to initialize us after spring is up)
            while ((config == null || config.getApplicationContext() == null) && (now() - start < STARTUP_TIMEOUT)) {
                sleep(SECONDS.toMillis(1));
            }
            if (config == null) {
                log.warn(simpleClass+": RestServerConfiguration was never set, exiting");
                stop(); return;
            }
            if (config.getApplicationContext() == null) {
                log.warn(simpleClass+": RestServerConfiguration.applicationContext was never set, exiting");
                stop(); return;
            }

            @Cleanup final BufferedReader reader;
            try {
                reader = new BufferedReader(new InputStreamReader(in));
            } catch (Exception e) {
                log.warn(simpleClass+": error setting up reader, exiting");
                stop(); return;
            }
            final LogRelayAppenderConfig logRelayConfig = config.getLogRelay();
            if (logRelayConfig == null) {
                log.warn(simpleClass+": no relayConfig was found, exiting");
                stop(); return;
            }
            final String relayTo = logRelayConfig.getRelayTo();
            if (empty(relayTo)) {
                log.warn(simpleClass+": relayConfig was found, but relayTo was empty, exiting");
                stop(); return;
            }
            final LogRelayAppenderTarget relayTarget;
            try {
                // cast shouldn't be required, but a compilation error occurs if we remove it
                relayTarget = (LogRelayAppenderTarget) config.getBean(relayTo);
                if (!relayTarget.init(logRelayConfig.getParams())) {
                    log.warn(simpleClass+": relayTo ("+ relayTo +") disabled, exiting");
                    stop(); return;
                }
            } catch (Exception e) {
                log.warn(simpleClass+": error initializing relayTo ("+ relayTo +"), exiting: "+e);
                stop(); return;
            }

            superStart();

            log.info(simpleClass+": starting log lines relay to LogRelayAppenderTarget spring bean: "+relayTarget.getClass().getName());
            String line = null;
            try {
                while ((line = reader.readLine()) != null) relayTarget.relay(line);
            } catch (Exception e) {
                stop();
                log.info(simpleClass+": stopping log lines relay to LogRelayAppenderTarget spring bean: "+relayTarget.getClass().getName());
                throw new IllegalStateException(simpleClass+": error relaying line ("+line+"), exiting: "+e);
            }
        });
    }

}
