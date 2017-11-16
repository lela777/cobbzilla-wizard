package org.cobbzilla.wizard.model.entityconfig;

import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.Map;

public interface ModelVerifyLog {

    String HTML_TEMPLATE = StringUtil.getPackagePath(ModelVerifyLog.class) + "/model_verify_template.html.hbs";

    void startLog();

    void logDifference(ApiClientBase api, Map<String, Identifiable> context, EntityConfig entityConfig, Identifiable existing, Identifiable entity);

    void logCreation(EntityConfig entityConfig, Identifiable entity);

    void endLog();

}
