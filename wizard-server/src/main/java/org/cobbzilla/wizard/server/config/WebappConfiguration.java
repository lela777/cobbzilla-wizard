package org.cobbzilla.wizard.server.config;

import com.sun.jersey.api.core.ClassNamesResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.ArrayUtil;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

public class WebappConfiguration {

    @Getter @Setter private String name;
    @Getter @Setter private String path;
    @Getter @Setter private boolean useServletContainer = false;
    @Getter @Setter private ServletConfiguration[] servlets;

    public WebappContext build(ConfigurableApplicationContext applicationContext) {
        final WebappContext context = new WebappContext(getName(), getPath());
        for (ServletConfiguration servletConfiguration : getServlets()) {
            final ServletRegistration registration = context.addServlet(servletConfiguration.getName(), ServletContainer.class);
            registration.setInitParameter(ServletContainer.RESOURCE_CONFIG_CLASS, ClassNamesResourceConfig.class.getName());
            registration.setInitParameter(ClassNamesResourceConfig.PROPERTY_CLASSNAMES, ArrayUtil.arrayToString(servletConfiguration.getClasses(), ", "));

//            final ResourceConfig rc = new PackagesResourceConfig(servletConfiguration.getResourcePackages());
//            final IoCComponentProviderFactory factory = new SpringComponentProviderFactory(rc, applicationContext);

            for (Map.Entry<String, String> initParam : servletConfiguration.getInitParams().entrySet()) {
                registration.setInitParameter(initParam.getKey(), initParam.getValue());
            }
            registration.setAsyncSupported(servletConfiguration.isAsyncSupported());
            registration.addMapping(servletConfiguration.getMapping());
        }
        return context;
    }

}
