package org.cobbzilla.wizard.resources;

import org.cobbzilla.wizard.model.Identifiable;

public interface EntityMappableResource {

    public Class<? extends Identifiable> getEntityClass();

}
