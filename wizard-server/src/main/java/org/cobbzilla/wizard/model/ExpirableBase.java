package org.cobbzilla.wizard.model;

import lombok.Getter;
import lombok.Setter;

public abstract class ExpirableBase extends IdentifiableBase {

    @Getter @Setter private long expirationSeconds;

}
