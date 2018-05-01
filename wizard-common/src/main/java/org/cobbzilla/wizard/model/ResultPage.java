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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
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
    public static final int INFINITE = Integer.MAX_VALUE;
    public static final ResultPage INFINITE_PAGE = new ResultPage(1, INFINITE);

    public static final ResultPage EMPTY_PAGE = new ResultPage(1, 0);
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
        if (pageNumber != null) setPageNumber(pageNumber);
        if (pageSize != null) setPageSize(pageSize);
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

    @Getter private int pageNumber = 1;
    public ResultPage setPageNumber(int pageNumber) { this.pageNumber = pageNumber <= 0 ? 1 : pageNumber; return this; }

    @Getter private int pageSize = 10;
    public ResultPage setPageSize(int pageSize) { this.pageSize = pageSize <= 0 ? 10 : pageSize; return this; }

    @JsonIgnore public int getPageOffset () { return (getPageNumber()-1) * pageSize; }
    public boolean containsResult(int i) { return (i >= getPageOffset() && i <= getPageOffset()+getPageSize()); }

    @JsonIgnore public boolean isInfinitePage () { return pageSize == INFINITE_PAGE.pageSize; }
    @JsonIgnore public int getPageEndOffset() {
        return isInfinitePage() ? INFINITE_PAGE.pageSize : getPageOffset() + getPageSize();
    }

    public ResultPage setReturnAllResults () { setPageNumber(INFINITE_PAGE.pageNumber); setPageSize(INFINITE_PAGE.pageSize); return this; }

    public static final int MAX_PAGE_BUFFER = 100;
    @JsonIgnore public int getPageBufferSize () {
        return isInfinitePage() || pageSize > MAX_PAGE_BUFFER ? MAX_PAGE_BUFFER : pageSize;
    }

    @Setter private String sortField = DEFAULT_SORT_FIELD;
    public String getSortField() {
        if (sortField.contains(";")) die("invalid sort: "+sortField);

        // only return the first several chars, to thwart a hypothetical injection attack
        // more sophisticated than the classic 'add a semi-colon then do something nefarious'
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
    @JsonIgnore public boolean getHasBounds() { return !empty(bounds); }
    public boolean hasBound (String name) { return getHasBounds() && bounds.containsKey(name); }

    public ResultPage setBound(String name, String value) {
        if (bounds == null) bounds = new LinkedHashMap<>();
        bounds.put(name, value);
        return this;
    }

    public void unsetBound(String name) { bounds.remove(name); }
    public void unsetBounds() { bounds.clear(); }

    @Getter @Setter private String[] fields;
    @JsonIgnore public boolean getHasFields () { return !empty(fields); }

    @JsonIgnore @Getter @Setter private SearchScrubber scrubber;
    public boolean hasScrubber () { return scrubber != null; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResultPage that = (ResultPage) o;

        if (getPageNumber() != that.getPageNumber()) return false;
        if (getPageSize() != that.getPageSize()) return false;
        if (!MapUtil.deepEquals(bounds, that.bounds)) return false;
        if (filter != null ? !filter.equals(that.filter) : that.filter != null) return false;
        if (sortField != null ? !sortField.equals(that.sortField) : that.sortField != null) return false;
        if (sortOrder != null ? !sortOrder.equals(that.sortOrder) : that.sortOrder != null) return false;
        if (fields != null && (that.fields == null || !Arrays.equals(that.fields, fields))) return false;
        if (fields == null && that.fields != null) return false;
        return true;
    }

    @Override public int hashCode() {
        int result = getPageNumber();
        result = 31 * result + getPageSize();
        result = 31 * result + (sortField != null ? sortField.hashCode() : 0);
        result = 31 * result + (sortOrder != null ? sortOrder.hashCode() : 0);
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + (bounds != null ? MapUtil.deepHash(bounds) : 0);
        result = 31 * result + (fields != null ? Arrays.hashCode(fields) : 0);
        return result;
    }
}
