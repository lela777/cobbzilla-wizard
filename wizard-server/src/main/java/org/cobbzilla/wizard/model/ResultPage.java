package org.cobbzilla.wizard.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.validation.ValidEnum;

public class ResultPage {

    public static final int MAX_FILTER_LENGTH = 50;
    public static final int MAX_SORTFIELD_LENGTH = 50;

    public enum SortOrder { ASC, DESC }

    public static final ResultPage DEFAULT_PAGE = new ResultPage();
    public static final ResultPage FIRST_RESULT = new ResultPage(1, 1, null, null, null);

    public ResultPage () {}

    public ResultPage (int pageNumber, int pageSize, String sortField, SortOrder sortOrder, String filter) {
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.sortField = sortField;
        this.sortOrder = (sortOrder == null) ? null : sortOrder.name();
        this.filter = filter;
    }

    @Getter @Setter
    private int pageNumber = 1;

    @JsonIgnore
    public int getPageOffset () { return pageNumber * pageSize; }

    @Getter @Setter
    private int pageSize = 10;

    @Setter
    private String sortField = "ctime";
    public String getSortField() {
        // only return the first several chars, to thwart a hypothetical injection attack.
        return StringUtil.prefix(sortField, MAX_SORTFIELD_LENGTH);
    }

    @ValidEnum(type=SortOrder.class, emptyOk=true, message= BasicConstraintConstants.ERR_SORT_ORDER_INVALID)
    @Getter @Setter
    private String sortOrder = SortOrder.DESC.name();

    @JsonIgnore
    public SortOrder getSortType () { return sortOrder == null ? null : SortOrder.valueOf(sortOrder); }

    @Setter
    private String filter = null;
    public String getFilter() {
        // only return the first several chars, to thwart a hypothetical injection attack.
        return StringUtil.prefix(filter, MAX_FILTER_LENGTH);
    }
    public boolean getHasFilter() { return filter != null && filter.trim().length() > 0; }

}
