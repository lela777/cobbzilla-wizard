package org.cobbzilla.wizard.model;

import javax.persistence.MappedSuperclass;
import java.util.UUID;

@MappedSuperclass
public class StrongIdentifiableBase extends IdentifiableBase {

    @Override public void initUuid() { setUuid(UUID.randomUUID().toString() + "-" + Long.toHexString(System.currentTimeMillis())); }

}
