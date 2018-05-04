package org.cobbzilla.wizard.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.time.TimeUtil;
import org.joda.time.format.DateTimeFormatter;

import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.string.StringUtil.sqlFilter;
import static org.cobbzilla.util.time.TimeUtil.*;

@AllArgsConstructor
public enum SearchBoundComparison {

    eq      (s -> s+" = ?",     SearchBoundComparison::parseCompareArgument),
    ne      (s -> s+" != ?",    SearchBoundComparison::parseCompareArgument),
    gt      (s -> s+" > ?",     SearchBoundComparison::parseCompareArgument),
    ge      (s -> s+" >= ?",    SearchBoundComparison::parseCompareArgument),
    lt      (s -> s+" < ?",     SearchBoundComparison::parseCompareArgument),
    le      (s -> s+" <= ?",    SearchBoundComparison::parseCompareArgument),
    before  (s -> s+" <= ?",    SearchBoundComparison::parseDateArgument),
    after   (s -> s+" >= ?",    SearchBoundComparison::parseDateArgument),
    like    (s -> s+" ilike ?", SearchBoundComparison::parseLikeArgument),
    custom  (null);

    private static Object parseLikeArgument(String val, SearchBoundComparison comparison, SearchFieldType type) { return sqlFilter(val); }

    private static Object parseCompareArgument(String val, SearchBoundComparison comparison, SearchFieldType type) {
        if (type == null) {
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

    private static Object parseDateArgument(String val, SearchBoundComparison comparison, SearchFieldType type) {
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
            return die("parseDateArgument: '"+val+"' is not a valid date or epoch time");
        }
    }

    private Function<String, String> sqlFunction;
    private SearchBoundValueFunction valueFunction;

    SearchBoundComparison(Function<String, String> sqlFunction) { this.sqlFunction = sqlFunction; }

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

    public Object prepareValue(String value, SearchFieldType type) { return valueFunction != null ? valueFunction.paramValue(value, this, type) : value; }

    public String sql(SearchBound bound) { return sqlFunction.apply(bound.getName()); }

}
