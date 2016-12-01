package org.cobbzilla.wizard.model.entityconfig;

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.util.RestResponse;

public interface ModelSetupListener {

    void preCreate (EntityConfig entityConfig, Identifiable entity);
    void postCreate(EntityConfig entityConfig, Identifiable entity, Identifiable created);

    void preUpdate (EntityConfig entityConfig, Identifiable entity);
    void postUpdate(EntityConfig entityConfig, Identifiable entity, Identifiable created);

    String preEntityConfig (String entityType, String json);
    String postEntityConfig(String entityType, EntityConfig entityConfig, String json);

    void preLookup(Identifiable entity);
    void postLookup(Identifiable entity, Identifiable request, RestResponse response);

}
