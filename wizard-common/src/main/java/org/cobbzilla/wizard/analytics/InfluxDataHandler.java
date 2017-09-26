package org.cobbzilla.wizard.analytics;

public class InfluxDataHandler extends AnalyticsHandlerBase {

    public String getWriteUrl() {
        return new StringBuilder(config.getHost()).append(":").append(config.getPort()).append("/write?db=").append(config.getEnv()).append("&u=").append(config.getUsername()).append("&p=").append(config.getPassword()).append("&precision=ms").toString();
    }

}
