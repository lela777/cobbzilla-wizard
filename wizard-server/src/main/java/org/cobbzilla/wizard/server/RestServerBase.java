package org.cobbzilla.wizard.server;

import com.google.common.collect.Lists;
import com.sun.jersey.api.container.ContainerFactory;
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
import org.cobbzilla.util.system.PortPicker;
import org.cobbzilla.wizard.server.config.HttpConfiguration;
import org.cobbzilla.wizard.server.config.JerseyConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.cobbzilla.wizard.server.config.StaticHttpConfiguration;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.FileConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.validation.Validator;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.cobbzilla.util.string.StringUtil.EMPTY_ARRAY;

@Slf4j
public abstract class RestServerBase<C extends RestServerConfiguration> implements RestServer<C> {

    @Getter private HttpServer httpServer;
    @Getter @Setter private C configuration;

    private ConfigurableApplicationContext applicationContext;
    public ApplicationContext getApplicationContext () { return applicationContext; }

    private boolean hasPort () {
        return configuration != null && configuration.getHttp() != null && configuration.getHttp().getPort() != 0;
    }

    private void verifyPort() { if (!hasPort()) throw new IllegalStateException("no http port specified"); }

    public URI getBaseUri() { return buildURI(getListenAddress()); }

    protected String getListenAddress() { return ALL_ADDRS; }

    @Override
    public String getClientUri() {
        verifyPort();
        return buildURI(LOCALHOST).toString();
    }

    protected URI buildURI(String host) {
        verifyPort();
        HttpConfiguration httpConfiguration = configuration.getHttp();
        return UriBuilder.fromUri("http://" + host + httpConfiguration.getBaseUri()).port(httpConfiguration.getPort()).build();
    }

    public HttpServer startServer() throws IOException {

        final String serverName = configuration.getServerName();
        buildServer(serverName);

        // fire it up
        log.info("starting "+serverName+"...");
        httpServer.start();
        // httpServer = GrizzlyServerFactory.createHttpServer(getBaseUri(), rc, factory);
        log.info(serverName+" started.");
        return httpServer;
    }

    public HttpServer buildServer(String serverName) throws IOException {
        final JerseyConfiguration jerseyConfiguration = configuration.getJersey();
        final ResourceConfig rc = new PackagesResourceConfig(jerseyConfiguration.getResourcePackages());

        rc.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);

        if (jerseyConfiguration.hasRequestFilters()) {
            rc.getProperties().put("com.sun.jersey.spi.container.ContainerRequestFilters",
                                    Lists.newArrayList(jerseyConfiguration.getRequestFilters()));
        }
        if (jerseyConfiguration.hasResponseFilters()) {
            rc.getProperties().put("com.sun.jersey.spi.container.ContainerResponseFilters",
                                    Lists.newArrayList(jerseyConfiguration.getResponseFilters()));
        }

        rc.getSingletons().add(new JacksonMessageBodyProvider(JsonUtil.NOTNULL_MAPPER, new Validator()));
        rc.getSingletons().add(new StreamingOutputProvider());

        BCryptUtil.setBcryptRounds(configuration.getBcryptRounds());

        applicationContext = buildSpringApplicationContext();
        configuration.setApplicationContext(applicationContext);

        // tell grizzly where the IoC factory is coming from
        final IoCComponentProviderFactory factory = new SpringComponentProviderFactory(rc, applicationContext);

        // pick a port
        if (configuration.getHttp().getPort() == 0) {
            configuration.getHttp().setPort(PortPicker.pick());
        }

        httpServer = new HttpServer();
        final NetworkListener listener = new NetworkListener("grizzly-"+serverName, getListenAddress(), configuration.getHttp().getPort());
        httpServer.addListener(listener);

        final HttpHandler processor = ContainerFactory.createContainer(HttpHandler.class, rc, factory);
        final String restBase = configuration.getHttp().getBaseUri();
        httpServer.getServerConfiguration().addHttpHandler(processor, restBase);

        if (configuration.hasStaticAssets()) {
            final StaticHttpConfiguration staticAssets = configuration.getStaticAssets();
            final String staticBase = staticAssets.getBaseUri();
            if (staticBase == null || staticBase.trim().length() == 0 || staticBase.trim().equals(restBase)) {
                throw new IllegalArgumentException("staticAssetBaseUri not defined, or is same as restServerBaseUri");
            }
            final StaticAssetHandler staticHandler = new StaticAssetHandler(staticAssets, getClass().getClassLoader());
            httpServer.getServerConfiguration().addHttpHandler(staticHandler, staticBase);
        }

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

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> S main(String[] args,
                                                                                          Class<S> mainClass) throws Exception {
        return main(args, mainClass, null, getConfigurationSources(args));
    }

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> S
                                                        main(Class<S> mainClass,
                                                             List<ConfigurationSource> configSources) throws Exception {
        return main(EMPTY_ARRAY, mainClass, null, configSources);
    }

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> S
                                                        main(String[] args,
                                                             Class<S> mainClass,
                                                             List<ConfigurationSource> configSources) throws Exception {
        return main(args, mainClass, null, configSources);
    }

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> S
                                                        main(Class<S> mainClass,
                                                             final RestServerLifecycleListener<S> listener,
                                                             List<ConfigurationSource> configSources) throws Exception {
        return main(EMPTY_ARRAY, mainClass, listener, configSources);
    }

    public static <S extends RestServerBase<C>, C extends RestServerConfiguration> S
                                                        main(String[] args, Class<S> mainClass,
                                                             final RestServerLifecycleListener<S> listener,
                                                             List<ConfigurationSource> configSources) throws Exception {

        final Thread mainThread = Thread.currentThread();

        // Ignore "server" argument if it is the first arg
        final List<String> argList = Arrays.asList(args);
        if (argList.size() >= 1 && argList.get(0).equals("server")) {
            argList.remove(0);
        }

        final RestServerHarness<C, S> serverHarness = new RestServerHarness<>(mainClass);

        serverHarness.addConfigurations(configSources);
        serverHarness.init(System.getenv());

        final S server = getServer(serverHarness, listener);
        server.startServer();
        if (listener != null) listener.onStart(server);

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
                if (listener != null) listener.onStop(server);
            }
        }));

        log.info(serverName+" running, base URI is " + server.getBaseUri().toString());
        try {
            synchronized (mainThreadLock) { mainThreadLock.wait(); }
        } catch (InterruptedException e) {
            log.info(serverName+" server thread interrupted, stopping server...");
        }

        return server;
    }

    private static <S extends RestServerBase<C>, C extends RestServerConfiguration> S getServer(RestServerHarness<C, S> serverHarness,
                                                                                                RestServerLifecycleListener<S> listener) {
        S server = serverHarness.getServer();
        if (listener != null) server = listener.beforeStart(server);
        return server;
    }

    protected static List<ConfigurationSource> getConfigurationSources(String[] args) {
        return getFileConfigurationSources(args);
    }

    protected static List<ConfigurationSource> getFileConfigurationSources(String[] args) {
        final List<ConfigurationSource> sources = new ArrayList<>();
        for (String arg : args) {
            sources.add(new FileConfigurationSource(new File(arg)));
        }
        return sources;
    }

    protected static List<ConfigurationSource> getStreamConfigurationSources(Class clazz, String[] args) {
        return StreamConfigurationSource.fromResources(clazz, args);
    }

}
