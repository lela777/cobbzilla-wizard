package org.cobbzilla.wizard.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.io.DeleteOnExit;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.mustache.LocaleAwareMustacheFactory;
import org.cobbzilla.wizard.server.config.StaticHttpConfiguration;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.File;
import java.io.Writer;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;

@Slf4j
public class StaticAssetHandler extends CLStaticHttpHandler {

    public static final String[] DEFAULT_INDEX_ALIASES = {"/index.php"};

    private StaticHttpConfiguration configuration;
    private Set<String> indexAliases = new HashSet<>();
    private File assetDirFile;

    private File templateFileRoot;

    public StaticAssetHandler(StaticHttpConfiguration configuration, ClassLoader classLoader) {
        super(classLoader, configuration.getAssetRoot());
        this.configuration = configuration;
        if (configuration.hasLocalOverride()) {
            assetDirFile = configuration.getLocalOverride();
            if (assetDirFile.exists() && assetDirFile.canRead()) {
                templateFileRoot = assetDirFile;
                LocaleAwareMustacheFactory.setSkipClasspath(true);
            } else {
                log.warn("asset dir ("+abs(assetDirFile)+") does not exist, all resources will be served from classpath");
            }
        }

        // everything will load from the classpath.
        // create a dummy location where no templates will be found.
        if (templateFileRoot == null) {
            templateFileRoot = FileUtil.createTempDirOrDie(RandomStringUtils.randomAlphanumeric(20));
            DeleteOnExit.schedule(log, templateFileRoot);
        }

        final Map<String, String> utilPaths = configuration.getUtilPaths();
        if (utilPaths.containsKey(StaticUtilPath.INDEX_ALIASES.name())) {
            indexAliases = new HashSet<>(Arrays.asList(utilPaths.get(StaticUtilPath.INDEX_ALIASES.name()).split(":")));
        } else {
            indexAliases = new HashSet<>(Arrays.asList(DEFAULT_INDEX_ALIASES));
        }
        indexAliases.add("/"); // lonely slash always goes to index
        indexAliases.add("");  // empty path always goes to index
    }

    @Override
    protected boolean handle(String resourcePath, Request request, Response response) throws Exception {
        if (indexAliases.contains(resourcePath)) {
            return this.handle(getUtilPath(StaticUtilPath.INDEX_PATH, "/index.html"), request, response);
        }

        // remove double slashes
        while (resourcePath.contains("//")) resourcePath = resourcePath.replace("//", "/");

        if (isUtilPath(resourcePath, StaticUtilPath.REQUEST_HEADERS_JS, "/js/request_headers.js")) {
            final Writer writer = response.getWriter();
            final String data = getRequestHeaderJavascript(request);
            writer.write(data);
            return true;
        }

        if (isUtilPath(resourcePath, StaticUtilPath.LOCALIZE, "localize")) {

            String path = request.getQueryString();
            if (empty(path)) return false;

            path = path.replaceAll("[^A-Za-z0-9/_.-]", "x");
            if (path.startsWith("/")) path = path.substring(1);

            if (!configuration.isMustacheCacheEnabled()) LocaleAwareMustacheFactory.flushCache();
            final LocaleAwareMustacheFactory factory = LocaleAwareMustacheFactory.getFactory(templateFileRoot, request.getLocale());

            final Writer writer = response.getWriter();
            final Map<String, Object> scope = new HashMap<>();

            pickupContentType(response, path);
            if (factory.render(path, scope, writer)) return true;
            return factory.render(configuration.getMustacheResourceRoot()+path, scope, writer);
        }

        // ENV takes precedence
        if (assetDirFile != null) {
            File file = new File(assetDirFile, resourcePath);
            if (file.exists()) {
                if (file.isDirectory()) {
                    response.sendRedirect("index.html");
                    return true;
                }
                sendFile(response, file);
                return true;
            } else {
                log.info("resource "+resourcePath+" not found in override dir ("+abs(assetDirFile)+"), using default from classpath");
            }
        }

        return super.handle(resourcePath, request, response);
    }

    private String getUtilPath(StaticUtilPath utilPath, String defaultPath) {
        String path = configuration.getUtilPaths().get(utilPath.name());
        return (path != null) ? path : defaultPath;
    }

    private boolean isUtilPath(String resourcePath, StaticUtilPath utilPath, String defaultPath) {
        final String path = getUtilPath(utilPath, defaultPath);
        return resourcePath.equals(path);
    }

    private String getRequestHeaderJavascript(Request request) throws Exception {
        Map<String, Object> headers = new HashMap<>();
        for (String name : request.getHeaderNames()) {
            List<String> values = IteratorUtils.toList(request.getHeaders(name).iterator());
            if (values.size() == 1) {
                headers.put(name, values.get(0));
            } else {
                headers.put(name, values);
            }
        }
        return "REQUEST_HEADERS = " + JsonUtil.toJson(headers);
    }

}
