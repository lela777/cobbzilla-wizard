package org.cobbzilla.wizard.dao;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Slf4j
public class SqlViewSearchHelper {

    public static <T> SearchResults<T> search(SqlViewSearchableDAO<T> dao,
                                              ResultPage resultPage,
                                              RestServerConfiguration configuration) {

        final StringBuilder sql = new StringBuilder("from " + dao.getSearchView() + " where 1=1 ");

        final List<Object> params = new ArrayList<>();
        if (resultPage.getHasFilter()) {
            sql.append(" AND (").append(dao.buildFilter(resultPage, params)).append(") ");
        }
        if (resultPage.getHasBounds()) {
            for (String bound : resultPage.getBounds().keySet()) {
                sql.append(" AND (").append(dao.buildBound(bound, resultPage.getBounds().get(bound), params)).append(") ");
            }
        }

        final String sort;
        if (resultPage.getHasSortField()) {
            sort = dao.getSortField(resultPage.getSortField()) + " " + resultPage.getSortOrder();
        } else {
            sort = dao.getDefaultSort();
        }
        final String count = "select count(*) " + sql.toString();
        final String query = "select * " + sql.toString()
                + " ORDER BY " + sort
                + " LIMIT " + resultPage.getPageSize()
                + " OFFSET " + resultPage.getPageOffset();

        Integer totalCount = null;
        final List<T> things = new ArrayList<>();
        try {
            final Object[] args = params.toArray();
            totalCount = configuration.execSql(count, args).count();
            final ResultSetBean rs = configuration.execSql(query, args);
            for (Map<String, Object> row : rs.getRows()) {
                things.add(dao.buildResult(row));
            }
        } catch (Exception e) {
            log.warn("error determining total count: "+e);
        }

        return new SearchResults<>(things, totalCount);
    }

    public static Object getValue(Map<String, Object> row,
                                  String field,
                                  HibernatePBEStringEncryptor hibernateEncryptor,
                                  HashSet<String> encryptedColumns) {
        final Object value = row.get(field);
        return value == null || !encryptedColumns.contains(field) ? value : hibernateEncryptor.decrypt(value.toString());
    }

}
