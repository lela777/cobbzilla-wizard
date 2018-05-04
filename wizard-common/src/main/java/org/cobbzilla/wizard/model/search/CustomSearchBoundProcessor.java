package org.cobbzilla.wizard.model.search;

import java.util.List;

public interface CustomSearchBoundProcessor {

    String getOperation();

    String sql(String field, List<Object> params, String value);

}
