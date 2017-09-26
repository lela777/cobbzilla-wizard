package org.cobbzilla.wizard.analytics;

import lombok.NoArgsConstructor;

public abstract class AnalyticsHandlerBase implements AnalyticsHandler {

    protected AnalyticsConfiguration config;
    @Override public void init(AnalyticsConfiguration config) { this.config = config; }

}
