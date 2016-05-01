package org.cobbzilla.wizard.model.entityconfig;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class EntityFieldOption {

    @Getter @Setter private String value;
    @Getter @Setter private String displayValue;

}
