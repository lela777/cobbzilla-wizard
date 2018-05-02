package org.cobbzilla.wizard.model.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;

import java.util.function.Function;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@AllArgsConstructor
public enum SearchBoundComparison {

    eq      (s -> s+" = ?"),
    ne      (s -> s+" != ?"),
    like    (s -> s+" ilike ?"),
    gt      (s -> s+" > ?"),
    ge      (s -> s+" >= ?"),
    lt      (s -> s+" < ?"),
    le      (s -> s+" <= ?"),
    custom  (null);

    private Function<String, String> sqlFunction;

    public boolean isCustom() { return this == custom; }

    @JsonCreator public static SearchBoundComparison fromString (String val) { return valueOf(val.toLowerCase()); }

    public SearchBound bind(String name) {
        return this == custom
                ? die("bind: cannot bind name to custom comparison: "+name)
                : new SearchBound(name, this, null, null);
    }

    public String sql(String name) { return sqlFunction.apply(name); }

}
