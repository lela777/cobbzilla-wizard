package org.cobbzilla.wizard.thrift;

import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.cobbzilla.wizard.server.config.ThriftConfiguration;
import org.cobbzilla.wizard.util.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ThriftServerFactory {

    public static final ThriftServerFactory INSTANCE = new ThriftServerFactory();

    public List<ThriftServer> buildServers(ThriftConfiguration[] configurations, ApplicationContext applicationContext) {
        return buildServers(configurations, applicationContext, false);
    }

    public List<ThriftServer> buildServers(ThriftConfiguration[] configurations, ApplicationContext applicationContext, boolean start) {

        if (configurations == null || configurations.length == 0) return Collections.emptyList();

        final List<ThriftServer> servers = new ArrayList<>();
        for (ThriftConfiguration configuration : configurations) {
            servers.add(buildServer(configuration, applicationContext));
        }

        if (start) for (ThriftServer server : servers) server.start();

        return servers;
    }

    public ThriftServer buildServer(ThriftConfiguration configuration, ApplicationContext applicationContext) {

        final Object handler;
        final ThriftServer server = new ThriftServer();
        server.setConfiguration(configuration);
        try {
            final Class handlerClass = Class.forName(configuration.getHandler());
            handler = handlerClass.newInstance();
            SpringUtil.autowire(applicationContext, handler);

            final String serviceClass = configuration.getService();
            final Class ifaceClass = Class.forName(serviceClass + "$Iface");
            final Class processorClass = Class.forName(serviceClass +"$Processor");

            final TProcessor processor = (TProcessor) processorClass.getConstructor(ifaceClass).newInstance(handler);

            final TServerTransport serverTransport = new TServerSocket(configuration.getPort());
            server.setTServer(new TSimpleServer(new TServer.Args(serverTransport).processor(processor)));

        } catch (Exception e) {
            die("Error creating thrift tServer (" + configuration + "): " + e, e);
        }

        server.setThread(new Thread(server));

        return server;
    }

}
