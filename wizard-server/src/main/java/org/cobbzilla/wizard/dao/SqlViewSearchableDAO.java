package org.cobbzilla.wizard.dao;

import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.SqlViewField;

import java.util.List;
import java.util.Map;

import static org.cobbzilla.wizard.dao.AbstractDAO.getFilterString;

public interface SqlViewSearchableDAO<T extends Identifiable> extends DAO<T> {

    String getSearchView();

    default String fixedFilters() { return "1=1"; }

    SqlViewField[] getSearchFields();

    default String buildFilter(ResultPage resultPage, List<Object> params) {
        final String filter = getFilterString(resultPage.getFilter());
        final SqlViewField[] fields = getSearchFields();
        int filterCount = 0;
        final StringBuilder b = new StringBuilder();
        for (SqlViewField f : fields) {
            if (f.isUsedForFiltering()) {
                filterCount++;
                if (b.length() > 0) b.append(" OR ");
                b.append(f.getName()).append(" ilike ?");
            }
        }
        for (int i=0; i<filterCount; i++) params.add(filter);
        return b.toString();
    }

    String buildBound(String bound, String value, List<Object> params);

    String getSortField(String sortField);

    String getDefaultSort();

    T buildResult(Map<String, Object> row);

}
