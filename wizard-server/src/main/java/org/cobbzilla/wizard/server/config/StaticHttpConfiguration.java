package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class StaticHttpConfiguration {

    @Getter @Setter private String filesystemEnvVar;
    @Getter @Setter private String baseUri;
    @Getter @Setter private String[] assetRoots;

    public boolean hasAssetRoots() {
        return assetRoots != null && assetRoots.length > 0 && assetRoots[0].trim().length() > 0;
    }
}
