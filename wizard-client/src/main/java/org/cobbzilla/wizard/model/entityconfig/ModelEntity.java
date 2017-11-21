package org.cobbzilla.wizard.model.entityconfig;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.ArrayUtils;
import org.cobbzilla.wizard.model.Identifiable;

public interface ModelEntity extends Identifiable {
    ObjectNode jsonNode();
    void updateNode();
    boolean forceUpdate();
    boolean performSubstitutions();
    Identifiable getEntity();

    default boolean hasData() { return hasData(false); }

    default boolean hasData(boolean strict) {
        return IteratorUtils.toList(jsonNode().fieldNames()).stream().filter((n) -> !ArrayUtils.contains(excludeUpdateFields(strict), n)).count() > 0;
    }
}
