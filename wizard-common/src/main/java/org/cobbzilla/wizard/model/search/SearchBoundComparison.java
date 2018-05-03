package org.cobbzilla.wizard.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.time.TimeUtil;
import org.joda.time.format.DateTimeFormatter;

import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.time.TimeUtil.*;

@AllArgsConstructor
public enum SearchBoundComparison {

    eq      (s -> s+" = ?"),
    ne      (s -> s+" != ?"),
    gt      (s -> s+" > ?",     Long::valueOf),
    ge      (s -> s+" >= ?",    Long::valueOf),
    lt      (s -> s+" < ?",     Long::valueOf),
    le      (s -> s+" <= ?",    Long::valueOf),
    before  (s -> s+" <= ?",    SearchBoundComparison::parseDateArgument),
    after   (s -> s+" >= ?",    SearchBoundComparison::parseDateArgument),
    like    (s -> s+" ilike ?", StringUtil::sqlFilter),
    custom  (null);

    public static final DateTimeFormatter[] DATE_TIME_FORMATS = {
            DATE_FORMAT_YYYY_MM_DD, DATE_FORMAT_YYYY_MM_DD, DATE_FORMAT_YYYYMMDD,
            DATE_FORMAT_YYYY_MM_DD_HH_mm_ss, DATE_FORMAT_YYYYMMDDHHMMSS
    };
    private static Object parseDateArgument(String val) {
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
    private Function<String, Object> valueFunction;

    SearchBoundComparison(Function<String, String> sqlFunction) { this.sqlFunction = sqlFunction; }

    public boolean isCustom() { return this == custom; }

    @JsonCreator public static SearchBoundComparison fromString (String val) { return valueOf(val.toLowerCase()); }

    public static SearchBoundComparison fromStringOrNull (String val) {
        try { return fromString(val); } catch (Exception e) { return null; }
    }

    public SearchBound bind(String name) {
        return this == custom
                ? die("bind: cannot bind name to custom comparison: "+name)
                : new SearchBound(name, this, null, null);
    }

    public Object prepareValue(String value) { return valueFunction != null ? valueFunction.apply(value) : value; }

    public String sql(SearchBound bound) { return sqlFunction.apply(bound.getName()); }

}
