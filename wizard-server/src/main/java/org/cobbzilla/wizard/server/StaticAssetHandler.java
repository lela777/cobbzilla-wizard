package org.cobbzilla.wizard.server;

import org.apache.commons.collections.IteratorUtils;
import org.cobbzilla.util.json.JsonUtil;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaticAssetHandler extends CLStaticHttpHandler {

    private static final String ENV_ASSET_DIR = "STATIC_ASSETS_DIR";
    private String filesystemDirEnvVar;
    private String assetDir;
    private File assetDirFile;

    public StaticAssetHandler(String filesystemDirEnvVar, ClassLoader classLoader, String... docRoots) {
        super(classLoader, docRoots);
        this.filesystemDirEnvVar = filesystemDirEnvVar;
        if (this.filesystemDirEnvVar != null) {
            assetDir = System.getenv().get(filesystemDirEnvVar);
        }
    }

    @Override
    protected boolean handle(String resourcePath, Request request, Response response) throws Exception {

        if (resourcePath.equals("") || resourcePath.equals("/") || resourcePath.equals("/index.php")) {
            return this.handle("/index.html", request, response);
        }

        if (resourcePath.equals("/js/request_headers.js")) {
            response.setContentType("text/javascript");
            Map<String, Object> headers = new HashMap<>();
            for (String name : request.getHeaderNames()) {
                name = name.toLowerCase();
                List<String> values = IteratorUtils.toList(request.getHeaders(name).iterator());
                if (values.size() == 1) {
                    headers.put(name, values.get(0));
                } else {
                    headers.put(name, values);
                }
            }
            response.getWriter().write("REQUEST_HEADERS = " + JsonUtil.toJson(headers));
            return true;
        }

        // ENV takes precedence
        if (assetDir != null) {
            if (assetDirFile == null) assetDirFile = new File(assetDir);

            if (assetDirFile.exists()) {
                final File file = new File(assetDirFile.getAbsolutePath() + File.separator + resourcePath);
                if (file.exists()) {
                    StaticAssetHandler.sendFile(response, file);
                    return true;
                } else {
                    throw new IllegalStateException("asset dir ("+assetDirFile.getAbsolutePath()+") exists but file ("+file.getAbsolutePath()+") does not");
                }
            } else {
                throw new IllegalStateException("asset dir ("+assetDirFile.getAbsolutePath()+") does not exist");
            }
        }

        return super.handle(resourcePath, request, response);
    }

}
