package org.cobbzilla.wizard.model.search;

import java.util.List;

public interface SearchBoundSqlFunction {

    String generateSqlAndAddParams(SearchBound bound, List<Object> params, String value);

    static SearchBoundSqlFunction sqlCompare(String operator, SearchBoundValueFunction valueFunction) {
        return (bound, params, value) -> {
            params.add(valueFunction.paramValue(bound, value));
            return bound.getName() + " " + operator + " ?";
        };
    }

    static SearchBoundSqlFunction sqlAndCompare(String[] operators, SearchBoundValueFunction[] valueFunctions) {
        return (bound, params, value) -> {
            final StringBuilder b = new StringBuilder();
            for (int i = 0; i < operators.length; i++) {
                if (b.length() > 0) b.append(") AND (");
                b.append(bound.getName()).append(" ").append(operators[i]).append(" ?");
                params.add(valueFunctions[i].paramValue(bound, value));
            }
            return b.insert(0, "(").append(")").toString();
        };
    }

    static SearchBoundSqlFunction sqlNullCompare(boolean isNull) {
        return (bound, params, value) -> bound.getName() + " IS " + (!isNull ? "NOT " : "") + " NULL";
    }

}
