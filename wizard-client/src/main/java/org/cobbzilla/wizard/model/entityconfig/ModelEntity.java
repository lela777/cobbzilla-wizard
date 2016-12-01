package org.cobbzilla.wizard.model.entityconfig;

import org.cobbzilla.wizard.model.Identifiable;

public interface ModelEntity extends Identifiable {
    boolean allowUpdate();
    boolean performSubstitutions();
    Identifiable getEntity();
}
