package org.cobbzilla.wizard.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.json.JsonUtil;

import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class SearchResults<E> {

    public static JavaType jsonType(Class klazz) {
        return JsonUtil.PUBLIC_MAPPER.getTypeFactory().constructParametricType(SearchResults.class, klazz);
    }

    @Getter @Setter private List<E> results;
    @Getter @Setter private Integer totalCount;

    @JsonIgnore public int size() {
        if (totalCount == null) throw new IllegalStateException("size is unknown");
        return totalCount;
    }

    @JsonIgnore public boolean getHasResults () { return results != null && !results.isEmpty(); }
    @JsonIgnore public boolean getHasTotalCount () { return totalCount != null; }

    public SearchResults(List<E> results) { this.results = results; }

    public E getResult(int i) {
        return (i < 0 || i > results.size()-1) ? null : results.get(i);
    }
}
