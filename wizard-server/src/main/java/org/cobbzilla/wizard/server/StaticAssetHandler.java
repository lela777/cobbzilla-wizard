package org.cobbzilla.wizard.server;

import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

public class StaticAssetHandler extends CLStaticHttpHandler {

    public StaticAssetHandler(ClassLoader classLoader, String... docRoots) {
        super(classLoader, docRoots);
    }

    @Override
    protected boolean handle(String resourcePath, Request request, Response response) throws Exception {
        if (resourcePath.equals("") || resourcePath.equals("/") || resourcePath.equals("/index.php")) {
            return super.handle("/index.html", request, response);
        }
        return super.handle(resourcePath, request, response);
    }

}
