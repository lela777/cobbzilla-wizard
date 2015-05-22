package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.daemon.ZillaRuntime;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class StaticHttpConfiguration {

    @Getter @Setter private Map<String, String> utilPaths = new HashMap<>();
    @Getter @Setter private String baseUri;
    @Getter @Setter private String assetRoot;
    @Getter @Setter private File localOverride;
    @Getter @Setter private boolean mustacheCacheEnabled = true;
    @Getter @Setter private String mustacheResourceRoot;

    public boolean hasAssetRoot() { return !empty(assetRoot); }
    public boolean hasLocalOverride() { return localOverride != null; }

}
