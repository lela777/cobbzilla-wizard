package org.cobbzilla.wizard.model.entityconfig;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;

import java.util.List;

@Accessors(chain=true)
public class ModelDiffEntry {

    public ModelDiffEntry(String entityId) { this.entityId = entityId; }

    @Getter private final String entityId;
    @Getter @Setter private String jsonDiff;
    @Getter @Setter private List<String> deltas;
    @Getter @Setter private Identifiable createEntity;

}
