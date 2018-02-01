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

import java.util.*;

import static java.lang.Integer.min;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.model.ResultPage.DEFAULT_SORT;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Slf4j
public class SqlViewSearchHelper {

    public static <E extends Identifiable, R extends SqlViewSearchResult>
    SearchResults<E> search(SqlViewSearchableDAO<E> dao,
                            ResultPage resultPage,
                            Class<R> resultClass,
                            SqlViewField[] fields,
                            HibernatePBEStringEncryptor hibernateEncryptor,
                            RestServerConfiguration configuration) {

        final StringBuilder sql = new StringBuilder("from " + dao.getSearchView() + " where (").append(dao.fixedFilters()).append(") ");
        final StringBuilder sqlWithoutFilters = new StringBuilder(sql);

        final List<Object> params = new ArrayList<>();
        final List<Object> paramsForEncrypted = new ArrayList<>();

        if (resultPage.getHasFilter()) {
            sql.append(" AND (").append(dao.buildFilter(resultPage, params)).append(") ");
        }

        if (resultPage.getHasBounds()) {
            for (String bound : resultPage.getBounds().keySet()) {
                sql.append(" AND (").append(dao.buildBound(bound, resultPage.getBounds().get(bound), params))
                                    .append(") ");
                sqlWithoutFilters.append(" AND (")
                                 .append(dao.buildBound(bound, resultPage.getBounds().get(bound), paramsForEncrypted))
                                 .append(") ");
            }
        }

        boolean searchByEncryptedField = Arrays.stream(fields).anyMatch(a -> a.isUsedForFiltering() && a.isEncrypted());
        final String sort;
        final String sortedField;
        if (resultPage.getHasSortField()) {
            sortedField = dao.getSortField(resultPage.getSortField());
            sort = sortedField + " " + resultPage.getSortOrder();
        } else {
            sort = dao.getDefaultSort();
            sortedField = "ctime";
        }

        String offset = " OFFSET " + resultPage.getPageOffset();
        int limit = resultPage.getPageSize();
        if (searchByEncryptedField) {
            offset =  "";
            limit += resultPage.getPageSize();
        }

        final String uuidsSql = "select uuid " + sql.toString();
        final String query = "select * " + sql.toString()
                + " ORDER BY " + sort
                + " LIMIT " + limit
                + offset;
        Integer totalCount = null;
        final List<E> things = new ArrayList<>();
        final Set<String> allUuids = new HashSet<>();
        try {
            final Object[] args = params.toArray();
            ResultSetBean uuidsResult = configuration.execSql(uuidsSql, args);
            allUuids.addAll(uuidsResult.getColumnValues("uuid"));

            final ResultSetBean rs = configuration.execSql(query, args);
            for (Map<String, Object> row : rs.getRows()) {
                things.add((E) populate(instantiate(resultClass), row, fields, hibernateEncryptor));
            }

            if (searchByEncryptedField) {
                paramsForEncrypted.add(allUuids.toArray());
                final Object[] argsForEncrypted = paramsForEncrypted.toArray();
                final String queryForEncrypted = "select * " + sqlWithoutFilters.toString()
                        + "AND uuid NOT IN (SELECT * FROM unnest( ? )) "
                        + " ORDER BY " + sort;

                final ResultSetBean rsEncrypted = configuration.execSql(queryForEncrypted, argsForEncrypted);
                for (Map<String, Object> row : rsEncrypted.getRows()) {
                    E thing = (E) populateAndFilter(instantiate(resultClass), row, fields, hibernateEncryptor,
                                                    resultPage.getFilter());
                    if (!empty(thing)) {
                        if (!allUuids.contains(thing.getUuid())) {
                            things.add(thing);
                            allUuids.add(thing.getUuid());
                        }
                    }
                    if (things.size() == resultPage.getPageOffset() + resultPage.getPageSize()) break;
                }

                if (!resultPage.getSortOrder().equals(DEFAULT_SORT)) {
                    things.sort(new Comparator<E>() {
                        @Override public int compare(E o1, E o2) { return compareSelectedItems(o1, o2, sortedField); }
                    });
                } else {
                    things.sort(new Comparator<E>() {
                        @Override public int compare(E o1, E o2) { return compareSelectedItems(o1, o2, sortedField); }
                    }.reversed());
                }
            }

        } catch (Exception e) {
            log.warn("error determining total count: "+e);
        }

        totalCount = allUuids.size();
        int startIndex = resultPage.getPageOffset();
        if (things.size() < startIndex) {
            return new SearchResults<>(new ArrayList<>(), totalCount);
        } else {

            int endIndex = min(resultPage.getPageSize() + resultPage.getPageOffset(), things.size());
            return new SearchResults<>(things.subList(startIndex, endIndex), totalCount);
        }
    }

    private static <E extends Identifiable> int compareSelectedItems(E o1, E o2, String sortedField) {
        Object fieldObject1 = ReflectionUtil.get(o1, sortedField);
        Object fieldObject2 = ReflectionUtil.get(o2, sortedField);
        Class sortedFieldClass = ReflectionUtil.getSimpleClass(fieldObject1);

        if (sortedFieldClass.equals(String.class)) {
            return ((String) fieldObject1).compareTo((String) fieldObject2);
        } else if (sortedFieldClass.equals(Long.class)) {
            return ((Long) fieldObject1).compareTo((Long) fieldObject2);
        } else if (sortedFieldClass.equals(Integer.class)) {
            return ((Integer) fieldObject1).compareTo((Integer) fieldObject2);
        } else if (sortedFieldClass.equals(Boolean.class)) {
            return ((Boolean) fieldObject1).compareTo((Boolean) fieldObject2);
        }
        throw invalidEx("Sort field has invalid type");
    }

    public static <T extends SqlViewSearchResult, E extends Identifiable> T populateAndFilter(T thing,
        Map<String, Object> row, SqlViewField[] fields, HibernatePBEStringEncryptor hibernateEncryptor, String filter) {
        boolean containsFilterValue = false;
        for (SqlViewField field : fields) {
            final Class<? extends Identifiable> type = field.getType();
            Object target = thing;
            if (type != null) {
                // sanity check, should never happen
                if (!field.hasEntity()) die("populate: type was "+type.getName()+" but entity was null: "+field);
                target = thing.getRelated().entity(type, field.getEntity());
            }
            final Object value = getValue(row, field.getName(), hibernateEncryptor, field.isEncrypted());
            if (!containsFilterValue && !empty(value)
                && field.isUsedForFiltering()
                && value.toString().toLowerCase().contains(filter.toLowerCase())) {
                containsFilterValue = true;
            }
            if (field.hasSetter()) {
                field.getSetter().set(target, field.getEntityProperty(), value);
            } else {
                ReflectionUtil.set(target, field.getEntityProperty(), value);
            }
        }
        return containsFilterValue ? thing : null;
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

    public static Object getValue(Map<String, Object> row,
                                  String field,
                                  HibernatePBEStringEncryptor hibernateEncryptor,
                                  boolean encrypted) {
        final Object value = row.get(field);
        return value == null || !encrypted ? value : hibernateEncryptor.decrypt(value.toString());
    }
}
