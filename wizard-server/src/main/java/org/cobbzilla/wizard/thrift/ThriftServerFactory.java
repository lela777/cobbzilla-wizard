package org.cobbzilla.wizard.thrift;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.cobbzilla.wizard.server.config.ThriftConfiguration;
import org.cobbzilla.wizard.util.SpringUtil;
import org.springframework.context.ApplicationContext;

import java.util.HashMap;
import java.util.Map;

public class ThriftServerFactory {

    public Map<ThriftConfiguration, TServer> buildServers(ThriftConfiguration[] configurations, ApplicationContext applicationContext) {

        if (configurations == null || configurations.length == 0) return Collections.emptyMap();

        final Map<ThriftConfiguration, TServer> servers = new HashMap<>();
        for (ThriftConfiguration configuration : configurations) {
            servers.put(configuration, buildServer(configuration, applicationContext));
        }
        return servers;
    }

    public TServer buildServer(ThriftConfiguration configuration, ApplicationContext applicationContext) {

        final Object handler;
        final TServer server;
        try {
            final Class handlerClass = Class.forName(configuration.getHandler());
            handler = handlerClass.newInstance();
            SpringUtil.autowire(applicationContext, handler);

            final String serviceClass = configuration.getService();
            final Class ifaceClass = Class.forName(serviceClass + "$Iface");
            final Class processorClass = Class.forName(serviceClass +"$Processor");

            final TProcessor processor = (TProcessor) processorClass.getConstructor(ifaceClass).newInstance(handler);

            final TServerTransport serverTransport = new TServerSocket(configuration.getPort());
            server = new TSimpleServer(new TServer.Args(serverTransport).processor(processor));

        } catch (Exception e) {
            throw new IllegalStateException("Error creating thrift server ("+configuration+"): "+e, e);
        }
        return server;
    }

}
