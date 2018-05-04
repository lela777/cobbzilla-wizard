package org.cobbzilla.wizard.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.collection.ComparisonOperator;
import org.cobbzilla.util.time.PastTimePeriodType;
import org.cobbzilla.util.time.TimeUtil;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.sqlFilter;
import static org.cobbzilla.util.time.TimeUtil.*;
import static org.cobbzilla.wizard.model.search.SearchBoundSqlFunction.sqlAndCompare;
import static org.cobbzilla.wizard.model.search.SearchBoundSqlFunction.sqlCompare;

@AllArgsConstructor
public enum SearchBoundComparison {

    eq      (sqlCompare(ComparisonOperator.eq.sql, SearchBoundComparison::parseCompareArgument)),
    ne      (sqlCompare(ComparisonOperator.ne.sql, SearchBoundComparison::parseCompareArgument)),
    gt      (sqlCompare(ComparisonOperator.gt.sql, SearchBoundComparison::parseCompareArgument)),
    ge      (sqlCompare(ComparisonOperator.ge.sql, SearchBoundComparison::parseCompareArgument)),
    lt      (sqlCompare(ComparisonOperator.lt.sql, SearchBoundComparison::parseCompareArgument)),
    le      (sqlCompare(ComparisonOperator.le.sql, SearchBoundComparison::parseCompareArgument)),

    like    (sqlCompare( "ilike", SearchBoundComparison::parseLikeArgument)),

    before  (sqlCompare(ComparisonOperator.le.sql, SearchBoundComparison::parseDateArgument)),
    after   (sqlCompare(ComparisonOperator.ge.sql, SearchBoundComparison::parseDateArgument)),

    during  (sqlAndCompare(new String[] {
            ComparisonOperator.ge.sql,
            ComparisonOperator.le.sql
    }, new SearchBoundValueFunction[] {
            (bound, value) -> PastTimePeriodType.valueOf(value).start(),
            (bound, value) -> PastTimePeriodType.valueOf(value).end()
    })),

    custom  (null);

    private SearchBoundSqlFunction sqlFunction;

    private static Object parseLikeArgument(SearchBound bound, String val) { return sqlFilter(val); }

    private static Object parseCompareArgument(SearchBound bound, String val) {
        SearchFieldType type = bound.getType();
        if (type == null) {
            final SearchBoundComparison comparison = bound.getComparison();
            switch (comparison) {
                case eq: case ne: type = SearchFieldType.string;  break;
                default:          type = SearchFieldType.integer; break;
            }
        }
        switch (type) {
            case flag:            return Boolean.parseBoolean(val);
            case integer:         return Long.parseLong(val);
            case decimal:         return Double.parseDouble(val);
            case string: default: return val;
        }
    }

    public static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DATE_FORMAT_YYYY_MM_DD, DATE_FORMAT_YYYY_MM_DD, DATE_FORMAT_YYYYMMDD,
            DATE_FORMAT_YYYY_MM_DD_HH_mm_ss, DATE_FORMAT_YYYYMMDDHHMMSS
    };

    private static Object parseDateArgument(SearchBound bound, String val) {
        for (DateTimeFormatter f : DATE_TIME_FORMATS) {
            try {
                return TimeUtil.parse(val, f);
            } catch (Exception ignored) {
                // noop
            }
        }
        try {
            return Long.parseLong(val);
        } catch (Exception e) {
            throw SearchField.invalid("err.param.invalid", "parseDateArgument: '"+val+"' is not a valid date or epoch time", val);
        }
    }

    public boolean isCustom() { return this == custom; }

    @JsonCreator public static SearchBoundComparison fromString (String val) { return valueOf(val.toLowerCase()); }

    public static SearchBoundComparison fromStringOrNull (String val) {
        try { return fromString(val); } catch (Exception ignored) { return null; }
    }

    public SearchBound bind(String name) { return bind(name, null); }

    public SearchBound bind(String name, SearchFieldType type) {
        return this == custom
                ? die("bind: cannot bind name to custom comparison: "+name)
                : new SearchBound(name, this, type, null, null);
    }

    public String sql(SearchBound bound, List<Object> params, String value) {
        return sqlFunction.generateSqlAndAddParams(bound, params, value);
    }

}
