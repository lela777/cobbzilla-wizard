package org.cobbzilla.wizard.resources;

import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Produces(APPLICATION_JSON) @Slf4j
public abstract class OpenApiDocsResource {

    protected abstract String[] getPackagesToScan();

    @Getter(lazy=true) private final OpenAPI swagger = initSwagger();

    protected OpenAPI initSwagger() {
        final ApiDocsConfiguration apiConfig = new ApiDocsConfiguration(getPackagesToScan());
        return apiConfig.scan();
    }

    @GET @Path("/swagger.json")
    @Operation(description = "The swagger definition in JSON", hidden = true)
    public Response getListingJson() { return Response.ok().entity(getSwagger()).build(); }

    private class ApiDocsConfiguration implements OpenAPIConfiguration {
        @Getter private final JaxrsAnnotationScanner scanner = new JaxrsAnnotationScanner();
        private final String[] packagesToScan;

        public ApiDocsConfiguration(String[] packagesToScan) {
            this.packagesToScan = packagesToScan;
            scanner.setConfiguration(this);
        }

        @Override public Set<String> getResourcePackages() { return new HashSet<>(Arrays.asList(packagesToScan)); }

        @Override public Set<String> getResourceClasses() {
            final Set<String> classes = new HashSet<>();
            for (String pkg : getPackagesToScan()) {
                final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(Path.class));
                classes.addAll(scanner.findCandidateComponents(pkg).stream()
                        .map(BeanDefinition::getBeanClassName)
                        .collect(Collectors.toSet()));
            }
            return classes;
        }

        @Override public String getReaderClass() { return Reader.class.getName(); }

        @Override public String getScannerClass() { return scanner.getClass().getName(); }

        @Override public String getFilterClass() { return null; }

        @Override public Collection<String> getIgnoredRoutes() { return null; }

        @Override public OpenAPI getOpenAPI() { return getSwagger(); }

        @Override public Map<String, Object> getUserDefinedOptions() { return Collections.emptyMap(); }

        @Override public Boolean isReadAllResources() { return true; }

        @Override public Boolean isPrettyPrint() { return true; }

        @Override public Long getCacheTTL() { return TimeUnit.DAYS.toMillis(1); }

        public OpenAPI scan() {
            final OpenAPI api = new OpenAPI();
            final Reader reader = new Reader(api);
            final Set<Class<?>> classes = scanner.classes();
            return reader.read(classes);
        }
    }
}
