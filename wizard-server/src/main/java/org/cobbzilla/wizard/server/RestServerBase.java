package org.cobbzilla.wizard.server;

import com.google.common.collect.Lists;
import com.sun.jersey.api.container.grizzly2.GrizzlyServerFactory;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.impl.provider.entity.StreamingOutputProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.spi.spring.container.SpringComponentProviderFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.security.bcrypt.BCryptUtil;
import org.cobbzilla.wizard.server.config.HttpConfiguration;
import org.cobbzilla.wizard.server.config.JerseyConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.factory.FileConfigurationSource;
import org.cobbzilla.wizard.validation.JacksonMessageBodyProvider;
import org.cobbzilla.wizard.validation.Validator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class RestServerBase<C extends RestServerConfiguration> implements RestServer<C> {

    private HttpServer httpServer;

    @Getter @Setter private C configuration;

    public URI getBaseUri() { return buildURI("0.0.0.0"); }

    @Override
    public String getClientUri() { return buildURI("127.0.0.1").toString(); }

    protected URI buildURI(String host) {
        HttpConfiguration httpConfiguration = configuration.getHttp();
        return UriBuilder.fromUri("http://" + host + httpConfiguration.getBaseUri()).port(httpConfiguration.getPort()).build();
    }

    public HttpServer startServer() throws IOException {

        JerseyConfiguration jerseyConfiguration = configuration.getJersey();
        ResourceConfig rc = new PackagesResourceConfig(jerseyConfiguration.getResourcePackages());

        rc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        rc.getProperties().put("com.sun.jersey.spi.container.ContainerResponseFilters", Lists.newArrayList(jerseyConfiguration.getResponseFilters()));

        rc.getSingletons().add(new JacksonMessageBodyProvider(JsonUtil.NOTNULL_MAPPER, new Validator()));
        rc.getSingletons().add(new StreamingOutputProvider());

        BCryptUtil.setBcryptRounds(configuration.getBcryptRounds());

        ConfigurableApplicationContext cac = buildSpringApplicationContext();

        // tell grizzly where the IoC factory is coming from
        IoCComponentProviderFactory factory = new SpringComponentProviderFactory(rc, cac);

        // fire it up
        log.info("starting "+configuration.getServerName()+"...");
        httpServer = GrizzlyServerFactory.createHttpServer(getBaseUri(), rc, factory);
        log.info(configuration.getServerName()+" started.");
        return httpServer;
    }

    protected ConfigurableApplicationContext buildSpringApplicationContext() {

        // Create a special factory that will always correctly resolve this specific configuration
        final DefaultListableBeanFactory factory = new DefaultListableBeanFactory() {
            @Override
            protected Object doResolveDependency(DependencyDescriptor descriptor, Class<?> type, String beanName, Set<String> autowiredBeanNames, TypeConverter typeConverter) throws BeansException {
                if (type.isAssignableFrom(configuration.getClass())) {
                    return configuration;
                }
                return super.doResolveDependency(descriptor, type, beanName, autowiredBeanNames, typeConverter);
            }
        };

        // Create a special context that uses the above factory
        final ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext() {
            @Override
            protected DefaultListableBeanFactory createBeanFactory() {
                return factory;
            }
        };

        // create the full context, with the config bean now "predefined"
        applicationContext.setConfigLocation(configuration.getSpringContextPath());
        applicationContext.refresh();
        return applicationContext;
    }

    public void stopServer() {
        log.info("stopping "+configuration.getServerName()+"...");
        httpServer.stop();
        log.info(configuration.getServerName()+" stopped.");
    }

    private static final Object mainThreadLock = new Object();

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> void main(String[] args, Class<S> mainClass) throws Exception {

        final Thread mainThread = Thread.currentThread();

        // Ignore "server" argument if it is the first arg
        final List<String> argList = Arrays.asList(args);
        if (argList.size() >= 1 && argList.get(0).equals("server")) {
            argList.remove(0);
        }

        final RestServerHarness<C, S> serverHarness = new RestServerHarness<>(mainClass);

        for (String arg : argList) {
            serverHarness.addConfiguration(new FileConfigurationSource(new File(arg)));
        }

        serverHarness.init(System.getenv());
        final S server = serverHarness.getServer();
        server.startServer();

        final String serverName = server.getConfiguration().getServerName();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){
            @Override
            public void run() {
                log.info("stopping "+serverName);
                server.stopServer();
                synchronized (mainThreadLock) {
                    mainThread.interrupt();
                    mainThreadLock.notify();
                }
            }
        }));

        log.info(serverName+" running, base URI is " + server.getBaseUri().toString());
        try {
            synchronized (mainThreadLock) { mainThreadLock.wait(); }
        } catch (InterruptedException e) {
            log.info(serverName+" server thread interrupted, stopping server...");
        }
    }

}
