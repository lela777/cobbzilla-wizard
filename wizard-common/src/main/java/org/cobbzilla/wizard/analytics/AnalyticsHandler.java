package org.cobbzilla.wizard.analytics;

public interface AnalyticsHandler {

    void init(AnalyticsConfiguration config);
    String getWriteUrl();

}
