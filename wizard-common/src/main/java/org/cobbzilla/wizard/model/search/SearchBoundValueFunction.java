package org.cobbzilla.wizard.model.search;

public interface SearchBoundValueFunction {

    Object paramValue(String value, SearchBoundComparison comparison, SearchFieldType type);

}
