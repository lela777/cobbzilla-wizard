package org.cobbzilla.wizard.dao;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;

import java.sql.SQLException;
import java.util.List;

public class HibernateCallbackImpl implements HibernateCallback<List> {

    private String queryString;
    private String[] paramNames;
    private Object[] values;

    private int firstResult;
    private int maxResults;

    /**
     * Fetches a {@link List} of entities from the database using pagination.
     * Execute HQL query, binding a number of values to ":" named parameters in the query string.
     *
     * @param queryString a query expressed in Hibernate's query language
     * @param paramNames the names of the parameters
     * @param values the values of the parameters
     * @param firstResult a row number, numbered from 0
     * @param maxResults the maximum number of rows
     */
    public HibernateCallbackImpl(
            String queryString,
            String[] paramNames,
            Object[] values,
            int firstResult,
            int maxResults) {
        this.queryString = queryString;
        this.paramNames = paramNames;
        this.values = values;

        this.firstResult = firstResult;
        this.maxResults = maxResults;
    }

    @Override
    public List doInHibernate(Session session) throws HibernateException,
            SQLException {
        Query query = session.createQuery(queryString);
        query.setFirstResult(firstResult);
        query.setMaxResults(maxResults);

        // TODO: throw proper exception when paramNames.length != values.length

        for (int c=0; c<paramNames.length; c++) {
            query.setParameter(paramNames[c], values[c]);
        }

        @SuppressWarnings("unchecked")
        List result = query.list();

        return result;
    }

}