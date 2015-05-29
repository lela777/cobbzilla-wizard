package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.MapUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.validation.ValidEnum;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @Accessors(chain=true) @ToString
public class ResultPage {

    public static final String ASC = SortOrder.ASC.toString();
    public static final String DESC = SortOrder.DESC.toString();

    public static final String PARAM_USE_PAGINATION = "page";
    public static final String PARAM_PAGE_NUMBER    = "pn";
    public static final String PARAM_PAGE_SIZE      = "ps";
    public static final String PARAM_SORT_FIELD     = "sf";
    public static final String PARAM_SORT_ORDER     = "so";
    public static final String PARAM_FILTER         = "q";
    public static final String PARAM_BOUNDS         = "b";

    public static final int MAX_FILTER_LENGTH = 50;
    public static final int MAX_SORTFIELD_LENGTH = 50;
    public static final String DEFAULT_SORT_FIELD = "ctime";

    public enum SortOrder {
        ASC, DESC;
        @JsonCreator public static SortOrder create(String val) { return valueOf(val.toUpperCase()); }
        public boolean isAscending () { return this == ASC; }
        public boolean isDescending () { return this == DESC; }
    }
    public static final String DEFAULT_SORT = SortOrder.DESC.name();

    public static final ResultPage DEFAULT_PAGE = new ResultPage();
    public static final ResultPage FIRST_RESULT = new ResultPage(1, 1);
    public static final ResultPage INFINITE_PAGE = new ResultPage(1, Integer.MAX_VALUE);

    public static final ResultPage LARGE_PAGE = new ResultPage(1, 100);

    // for using ResultPage as a query-parameter
    public static ResultPage valueOf (String json) throws Exception {
        return JsonUtil.fromJson(json, ResultPage.class);
    }

    public ResultPage(ResultPage other) {
        this.setPageNumber(other.getPageNumber());
        this.setPageSize(other.getPageSize());
        this.setFilter(other.getFilter());
        this.setSortField(other.getSortField());
        this.setSortOrder(other.getSortOrder());
        this.setBounds(other.getBounds());
    }

    public ResultPage(Integer pageNumber, Integer pageSize, String sortField, String sortOrder, String filter, Map<String, String> bounds) {
        if (pageNumber != null) this.pageNumber = pageNumber;
        if (pageSize != null) this.pageSize = pageSize;
        if (sortField != null) this.sortField = sortField;
        if (sortOrder != null) this.sortOrder = SortOrder.valueOf(sortOrder).name();
        if (filter != null) this.filter = filter;
        this.bounds = bounds;
    }

    public ResultPage (int pageNumber, int pageSize) {
        this(pageNumber, pageSize, null, normalizeSortOrder(null), null);
    }

    public ResultPage (int pageNumber, int pageSize, String sortField, SortOrder sortOrder) {
        this(pageNumber, pageSize, sortField, sortOrder, null);
    }

    public ResultPage (int pageNumber, int pageSize, String sortField, SortOrder sortOrder, String filter) {
        this(pageNumber, pageSize, sortField, normalizeSortOrder(sortOrder), filter, null);
    }

    public ResultPage (int pageNumber, int pageSize, String sortField, String sortOrder, String filter) {
        this(pageNumber, pageSize, sortField, sortOrder, filter, null);
    }

    public ResultPage (int pageNumber, int pageSize, String sortField, SortOrder sortOrder, String filter, Map<String, String> bounds) {
        this(pageNumber, pageSize, sortField, normalizeSortOrder(sortOrder), filter, bounds);
    }

    private static String normalizeSortOrder(SortOrder sortOrder) {
        return (sortOrder == null) ? ResultPage.DEFAULT_SORT : sortOrder.name();
    }

    public static ResultPage singleResult (String sortField, SortOrder sortOrder) {
        return new ResultPage(1, 1, sortField, sortOrder);
    }

    public static ResultPage singleResult (String sortField, SortOrder sortOrder, String filter) {
        return new ResultPage(new Integer(1), new Integer(1), sortField, sortOrder, filter);
    }

    @Getter @Setter private int pageNumber = 1;
    @Getter @Setter private int pageSize = 10;

    @JsonIgnore public int getPageOffset () { return (pageNumber-1) * pageSize; }
    public boolean containsResult(int i) { return (i >= getPageOffset() && i <= getPageOffset()+getPageSize()); }

    @JsonIgnore public boolean isInfinitePage () { return pageSize == INFINITE_PAGE.pageSize; }
    @JsonIgnore public int getPageEndOffset() {
        return isInfinitePage() ? INFINITE_PAGE.pageSize : getPageOffset() + getPageSize();
    }

    public static final int MAX_PAGE_BUFFER = 100;
    @JsonIgnore public int getPageBufferSize () {
        return isInfinitePage() || pageSize > MAX_PAGE_BUFFER ? MAX_PAGE_BUFFER : pageSize;
    }

    @Setter private String sortField = DEFAULT_SORT_FIELD;
    public String getSortField() {
        // only return the first several chars, to thwart a hypothetical injection attack.
        final String sort = empty(sortField) ? DEFAULT_SORT_FIELD : sortField;
        return StringUtil.prefix(sort, MAX_SORTFIELD_LENGTH);
    }
    @JsonIgnore public boolean getHasSortField () { return sortField != null; }

    @ValidEnum(type=SortOrder.class, emptyOk=true, message=BasicConstraintConstants.ERR_SORT_ORDER_INVALID)
    @Getter private String sortOrder = ResultPage.DEFAULT_SORT;
    public ResultPage setSortOrder(Object thing) {
        if (thing == null) {
            sortOrder = null;
        } else if (thing instanceof SortOrder) {
            sortOrder = ((SortOrder) thing).name();
        } else {
            sortOrder = thing.toString();
        }
        return this;
    }

    @JsonIgnore public SortOrder getSortType () { return sortOrder == null ? null : SortOrder.valueOf(sortOrder); }

    public ResultPage sortAscending () { sortOrder = SortOrder.ASC.name(); return this; }
    public ResultPage sortDescending () { sortOrder = SortOrder.DESC.name(); return this; }

    @Setter private String filter = null;
    public String getFilter() {
        // only return the first several chars, to thwart a hypothetical injection attack.
        return StringUtil.prefix(filter, MAX_FILTER_LENGTH);
    }
    @JsonIgnore public boolean getHasFilter() { return filter != null && filter.trim().length() > 0; }

    @Getter @Setter private Map<String, String> bounds;
    @JsonIgnore public boolean getHasBounds() { return bounds != null && !bounds.isEmpty(); }

    public void setBound(String name, String value) {
        if (bounds == null) bounds = new LinkedHashMap<>();
        bounds.put(name, value);
    }

    public void unsetBound(String name) { bounds.remove(name); }

    @JsonIgnore @Getter @Setter private SearchScrubber scrubber;
    public boolean hasScrubber () { return scrubber != null; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultPage that = (ResultPage) o;

        if (pageNumber != that.pageNumber) return false;
        if (pageSize != that.pageSize) return false;
        if (!MapUtil.deepEquals(bounds, that.bounds)) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (sortField != null ? !sortField.equals(that.sortField) : that.sortField != null) return false;
        if (sortOrder != null ? !sortOrder.equals(that.sortOrder) : that.sortOrder != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = pageNumber;
        result = 31 * result + pageSize;
        result = 31 * result + (sortField != null ? sortField.hashCode() : 0);
        result = 31 * result + (sortOrder != null ? sortOrder.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (bounds != null ? MapUtil.deepHash(bounds) : 0);
        return result;
    }
}
