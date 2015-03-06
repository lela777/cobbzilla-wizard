package org.cobbzilla.wizard.thrift;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.server.TServer;
import org.cobbzilla.wizard.server.config.ThriftConfiguration;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Slf4j
public class ThriftServer implements Runnable {

    @Getter @Setter private ThriftConfiguration configuration;
    @Getter @Setter private TServer tServer;
    @Getter @Setter private Thread thread;

    public void start() { thread.start(); }

    @Override
    public void run() {
        try {
            tServer.serve();
        } catch (Exception e) {
            String msg = "error in run loop (config=" + configuration + "): " + e;
            log.error(msg, e);
            die(msg, e);
        }
    }

}
