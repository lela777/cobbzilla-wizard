package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class StaticHttpConfiguration {

    @Getter @Setter private Map<String, String> utilPaths = new HashMap<>();
    @Getter @Setter private String filesystemEnvVar;
    @Getter @Setter private String baseUri;
    @Getter @Setter private String assetRoot;
    @Getter @Setter private boolean mustacheCacheEnabled = true;

    public boolean hasAssetRoot() { return !StringUtil.empty(assetRoot); }
    public boolean hasFilesystemEnvVar() { return !StringUtil.empty(filesystemEnvVar); }

}
