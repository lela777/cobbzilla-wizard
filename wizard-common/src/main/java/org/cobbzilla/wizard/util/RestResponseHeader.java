package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class RestResponseHeader {

    @Getter @Setter private String name;
    @Getter @Setter private String value;

}
