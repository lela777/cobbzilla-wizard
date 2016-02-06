package org.cobbzilla.wizard.dao;

/**
 * Forked from dropwizard https://github.com/dropwizard/
 * https://github.com/dropwizard/dropwizard/blob/master/LICENSE
 */

import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.ResultPage;
import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.SimpleExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateTemplate;

import java.io.Serializable;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

/**
 * An abstract base class for Hibernate DAO classes.
 *
 * @param <E> the class which this DAO manages
 */
public abstract class AbstractDAO<E> implements DAO<E> {

    @Autowired protected HibernateTemplate hibernateTemplate;

    private final Class<?> entityClass;

    /**
     * Creates a new DAO with a given session provider.
     */
    public AbstractDAO() { this.entityClass = ReflectionUtil.getTypeParameter(getClass()); }

    /**
     * Creates a new {@link Criteria} query for {@code <E>}.
     *
     * @return a new {@link Criteria} query
     * @see Session#createCriteria(Class)
     */
    protected DetachedCriteria criteria() { return DetachedCriteria.forClass(getEntityClass()); }

    protected DetachedCriteria criteria(Class entityClass) { return DetachedCriteria.forClass(entityClass); }

    /**
     * Returns the entity class managed by this DAO.
     *
     * @return the entity class managed by this DAO
     */
    @SuppressWarnings("unchecked")
    public Class<E> getEntityClass() { return (Class<E>) entityClass; }

    /**
     * Creates a new instance of the entity class using the default constructor
     * @return a new instance of E
     */
    public E newEntity () { return (E) instantiate(entityClass); }

    /**
     * Convenience method to return a single instance that matches the criteria, or null if the
     * criteria returns no results.
     *
     * @param criteria the {@link Criteria} query to run
     * @return the single result or {@code null}
     * @throws HibernateException if there is more than one matching result
     * @see Criteria#uniqueResult()
     */
    @SuppressWarnings("unchecked")
    protected E uniqueResult(DetachedCriteria criteria) throws HibernateException {
        return (E) DAOUtil.uniqueResult(hibernateTemplate.findByCriteria(criteria));
    }

    protected E uniqueResult(SimpleExpression expression) {
        return uniqueResult(criteria().add(expression));
    }

    /**
     * Get the results of a {@link Criteria} query.
     *
     * @param criteria the {@link Criteria} query to run
     * @return the list of matched query results
     * @see Criteria#list()
     */
    @SuppressWarnings("unchecked")
    protected List<E> list(DetachedCriteria criteria) throws HibernateException {
        return (List<E>) hibernateTemplate.findByCriteria(checkNotNull(criteria));
    }

    /**
     * Return the persistent instance of {@code <E>} with the given identifier, or {@code null} if
     * there is no such persistent instance. (If the instance, or a proxy for the instance, is
     * already associated with the session, return that instance or proxy.)
     *
     * @param id an identifier
     * @return a persistent instance or {@code null}
     * @throws HibernateException
     * @see Session#get(Class, Serializable)
     */
    @SuppressWarnings("unchecked")
    @Override public E get(Serializable id) { return (E) hibernateTemplate.get(entityClass, checkNotNull(id)); }

    /**
     * Either save or update the given instance, depending upon resolution of the unsaved-value
     * checks (see the manual for discussion of unsaved-value checking).
     * <p/>
     * This operation cascades to associated instances if the association is mapped with
     * <tt>cascade="save-update"</tt>.
     *
     * @param entity a transient or detached instance containing new or updated state
     * @throws HibernateException
     * @see Session#saveOrUpdate(Object)
     */
//    @Transactional
    public E persist(E entity) throws HibernateException {
        hibernateTemplate.saveOrUpdate(checkNotNull(entity));
        return entity;
    }

    /**
     * Force initialization of a proxy or persistent collection.
     * <p/>
     * Note: This only ensures initialization of a proxy object or collection;
     * it is not guaranteed that the elements INSIDE the collection will be initialized/materialized.
     *
     * @param proxy a persistable object, proxy, persistent collection or {@code null}
     * @throws HibernateException if we can't initialize the proxy at this time, eg. the {@link Session} was closed
     */
    protected <T> T initialize(T proxy) throws HibernateException {
        if (!Hibernate.isInitialized(proxy)) {
            Hibernate.initialize(proxy);
        }
        return proxy;
    }

    public static final String entityAlias = "x";
    public static final String FILTER_PARAM = "filter";
    public static final String[] EMPTY_PARAMS = new String[0];
    public static final Object[] EMPTY_VALUES = new Object[0];
    public static final String[] PARAM_FILTER = new String[]{FILTER_PARAM};

    @Override public SearchResults<E> search(ResultPage resultPage) {
        return search(resultPage, getEntityClass().getSimpleName());
    }

    @Override public SearchResults<E> search(ResultPage resultPage, String entityType) {
        String filterClause = "";
        String[] params;
        Object[] values;
        if (resultPage.getHasFilter()) {
            params = PARAM_FILTER;
            values = new Object[] { getFilterString(resultPage.getFilter()) };
            filterClause = getFilterClause(entityAlias, FILTER_PARAM);
        } else {
            params = EMPTY_PARAMS;
            values = EMPTY_VALUES;
        }
        if (resultPage.getHasBounds()) {
            for (String bound : resultPage.getBounds().keySet()) {
                if (filterClause.length() > 0) filterClause += "and ";
                filterClause += formatBound(entityAlias, bound, resultPage.getBounds().get(bound));
            }
        }
        if (filterClause.length() > 0) filterClause = "where "+filterClause;

        final StringBuilder qBuilder = new StringBuilder().append("from ").append(getEntityClass().getSimpleName()).append(" ").append(entityAlias).append(" ").append(filterClause);

        final String countQuery = "select count(*) " + qBuilder.toString();
        final String query = qBuilder.append(" order by ").append(entityAlias).append(".").append(resultPage.getSortField()).append(" ").append(resultPage.getSortType().name()).toString();

        List<E> results = query(query, resultPage, params, values);
        final int totalCount = Integer.valueOf(""+query(countQuery, ResultPage.INFINITE_PAGE, params, values).get(0));

        // the caller may want the results filtered (remove sensitive fields)
        if (resultPage.hasScrubber() && !results.isEmpty()) {
            results = resultPage.getScrubber().scrub(results);
        }

        return new SearchResults<>(results, totalCount);
    }

    public List query(String queryString, ResultPage resultPage, String[] params, Object[] values) {
        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, params, values, resultPage.getPageOffset(), resultPage.getPageSize()));
    }

    protected String formatBound(String entityAlias, String bound, String value) { return notSupported("Invalid bound: " + bound); }

    public static String caseInsensitiveLike(String entityAlias, String filterParam, final String attribute) {
        return new StringBuilder().append("lower(").append(entityAlias).append(".").append(attribute).append(") LIKE lower(:").append(filterParam).append(") ").toString();
    }

    private static final String PCT = "%";
    private static final String ESC_PCT = "[%]";
    public static String getFilterString(String value) {
        // escape any embedded '%' chars, and then add '%' as the first and last chars
        return PCT + value.toLowerCase().replace(PCT, ESC_PCT) + PCT;
    }

    protected String getFilterClause(String entityAlias, String filterParam) { return StringUtil.EMPTY; }

}
