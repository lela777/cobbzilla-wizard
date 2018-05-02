package org.cobbzilla.wizard.model.search;

public interface CustomSearchBoundProcessor {

    String getOperation();

    String sql(String field, String value);

}
