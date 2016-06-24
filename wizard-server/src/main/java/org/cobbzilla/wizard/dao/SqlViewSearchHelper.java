package org.cobbzilla.wizard.dao;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.SqlViewField;
import org.cobbzilla.wizard.model.SqlViewSearchResult;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class SqlViewSearchHelper {

    public static <E extends Identifiable, R extends SqlViewSearchResult>
    SearchResults<E> search(SqlViewSearchableDAO<E> dao,
                            ResultPage resultPage,
                            Class<R> resultClass,
                            SqlViewField[] fields,
                            HibernatePBEStringEncryptor hibernateEncryptor,
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
        final List<E> things = new ArrayList<>();
        try {
            final Object[] args = params.toArray();
            totalCount = configuration.execSql(count, args).count();
            final ResultSetBean rs = configuration.execSql(query, args);
            for (Map<String, Object> row : rs.getRows()) {
                things.add((E) populate(instantiate(resultClass), row, fields, hibernateEncryptor));
            }
        } catch (Exception e) {
            log.warn("error determining total count: "+e);
        }

        return new SearchResults<>(things, totalCount);
    }

    public static <T extends SqlViewSearchResult> T populate(T thing,
                                                             Map<String, Object> row,
                                                             SqlViewField[] fields,
                                                             HibernatePBEStringEncryptor hibernateEncryptor) {
        for (SqlViewField field : fields) {
            final Class<? extends Identifiable> type = field.getType();
            Object target = thing;
            if (type != null) {
                if (!field.hasEntity()) die("populate: type was "+type.getName()+" but entity was null: "+field); // sanity check, should never happen
                target = thing.getRelated().entity(type, field.getEntity());
            }
            final Object value = getValue(row, field.getName(), hibernateEncryptor, field.isEncrypted());
            if (field.hasSetter()) {
                field.getSetter().set(target, field.getEntityProperty(), value);
            } else {
                ReflectionUtil.set(target, field.getEntityProperty(), value);
            }
        }
        return thing;
    }

    protected static Object getValue(Map<String, Object> row,
                                     String field,
                                     HibernatePBEStringEncryptor hibernateEncryptor,
                                     boolean encrypted) {
        final Object value = row.get(field);
        return value == null || !encrypted ? value : hibernateEncryptor.decrypt(value.toString());
    }
}
