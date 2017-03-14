package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.cobbzilla.wizard.model.Identifiable;

public interface ModelEntity extends Identifiable {
    ObjectNode jsonNode();
    boolean allowUpdate();
    boolean performSubstitutions();
    Identifiable getEntity();
}
