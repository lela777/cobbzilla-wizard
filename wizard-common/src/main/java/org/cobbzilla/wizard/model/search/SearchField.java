package org.cobbzilla.wizard.model.search;

import org.cobbzilla.wizard.validation.SimpleViolationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.model.search.SearchBoundComparison.custom;

public interface SearchField {

    String name();

    SearchBound[] getBounds();
    default boolean hasBounds() { return !empty(getBounds()); }

    List<String> getComparisons();

    String getSort();
    default boolean hasSort() { return !empty(getSort()); }

    default SearchBound getBound(SearchBoundComparison comparison) {
        if (hasBounds()) for (SearchBound b : getBounds()) if (b.getComparison() == comparison) return b;
        return null;
    }

    default SearchBound getCustomBound(String op) {
        if (hasBounds()) for (SearchBound b : getBounds()) if (b.getComparison() == custom && b.getProcessor().getOperation().equals(op)) return b;
        return null;
    }

    default List<String> initComparisons() {
        final List<String> list = new ArrayList<>();
        if (hasBounds()) for (SearchBound b : getBounds()) list.add(b.getComparison().name());
        return list;
    }

    static List<String> initSortFields(SearchField[] values) {
        final List<String> fields = new ArrayList<>();
        for (SearchField f : values) if (f.hasSort()) fields.add(f.name());
        return fields;
    }

    static Map<String, SearchBound[]> initBounds(SearchField[] values) {
        final Map<String, SearchBound[]> fields = new HashMap<>();
        for (SearchField f : values) if (f.hasBounds()) fields.put(f.name(), f.getBounds());
        return fields;
    }

    static String buildBound(SearchField field, String value, List<Object> params) {
        final String bound = field.name();
        if (!field.hasBounds()) throw invalid("err.bound.invalid", "bind is not valid", bound);

        final int colonPos = value.indexOf(":");
        final SearchBoundComparison comparison;
        if (colonPos != -1) {
            final String[] parts = value.split(":");
            final String comparisonName = parts[0];
            if (comparisonName.startsWith(SearchBoundComparison.custom.name())) {
                if (parts.length <= 1) throw invalid("err.bound.custom.invalid", "custom bound was missing argument", value);
                final SearchBound customBound = field.getCustomBound(parts[1]);
                params.add(parts[2]);
                return customBound.getProcessor().sql(bound, value);
            } else {
                comparison = SearchBoundComparison.valueOf(comparisonName);
                final SearchBound searchBound = field.getBound(comparison);
                if (searchBound == null) throw invalid("err.bound.operation.invalid", "invalid comparison for bound "+bound+": "+comparisonName, comparisonName);
                params.add(comparison.prepareValue(value.substring(colonPos+1)));
            }

        } else {
            comparison = field.getBounds()[0].getComparison();
            params.add(value);
        }

        return comparison.sql(bound);
    }

    static SimpleViolationException invalid(String messageTemplate, String message, String invalidValue) {
        return new SimpleViolationException(messageTemplate, message, invalidValue);
    }

}
