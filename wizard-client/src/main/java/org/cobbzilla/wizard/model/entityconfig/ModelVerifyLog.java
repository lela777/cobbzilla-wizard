package org.cobbzilla.wizard.model.entityconfig;

import org.cobbzilla.wizard.model.Identifiable;

public interface ModelVerifyLog {

    void logDifference(EntityConfig entityConfig, Identifiable existing, Identifiable entity);

    void logCreation(EntityConfig entityConfig, Identifiable entity);

}
