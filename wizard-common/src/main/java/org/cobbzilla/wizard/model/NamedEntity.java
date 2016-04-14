package org.cobbzilla.wizard.model;

import java.util.Comparator;

public interface NamedEntity {

    String getName ();

    Comparator<? extends NamedEntity> NAME_COMPARATOR = new Comparator<NamedEntity>() {
        @Override public int compare(NamedEntity o1, NamedEntity o2) {
            return o1.getName().compareToIgnoreCase(o2.getName());
        }
    };

}
