package org.cobbzilla.wizard.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.EntityFilter;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class ResultCollectorBase extends ArrayList implements ResultCollector {

    @Getter @Setter protected int maxResults = 200;
    @Getter @Setter protected EntityFilter entityFilter = null;

    @Override public List getResults() { return this; }

    @Override public boolean add(Object o) { return addResult(o); }

    @Override public boolean addResult(Object thing) {
        if (entityFilter == null || entityFilter.isAcceptable(thing)) {
            if (size() > getMaxResults()) {
                return false;
            }
            super.add(thing);
        }
        return true;
    }

}
