package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;

import java.util.List;
import java.util.Map;

public interface SqlViewSearchableDAO<T extends Identifiable> extends DAO<T> {
    String getSearchView();

    String fixedFilters();

    String buildFilter(ResultPage resultPage, List<Object> params);

    String buildBound(String bound, String value, List<Object> params);

    String getSortField(String sortField);

    String getDefaultSort();

    T buildResult(Map<String, Object> row);

}
