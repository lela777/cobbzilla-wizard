package org.cobbzilla.wizard.util;

import lombok.*;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @ToString
public class RestResponseHeader {

    @Getter @Setter private String name;
    @Getter @Setter private String value;

}
