package org.cobbzilla.wizard.dao;

import lombok.AllArgsConstructor;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.criterion.CriteriaQuery;
import org.hibernate.criterion.Criterion;
import org.hibernate.engine.spi.TypedValue;

/**
 * Adapted from: http://stackoverflow.com/a/28502619/1251543
 */
public class ResultLimit {

    private static final TypedValue[] NO_TYPES = new TypedValue[0];

    private ResultLimit () {}

    public static MaxResults maxResults(int maxResults) { return new MaxResults(maxResults); }
    public static FirstResult firstResult(int firstResult) { return new FirstResult(firstResult); }

    @AllArgsConstructor
    static class MaxResults implements Criterion {

        private final int max;

        @Override public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            criteria.setMaxResults(max);
            return "1 = 1";
        }

        @Override public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            return NO_TYPES;
        }
    }

    @AllArgsConstructor
    static class FirstResult implements Criterion {

        private final int first;

        @Override public String toSqlString(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            criteria.setFirstResult(first);
            return "1 = 1";
        }

        @Override public TypedValue[] getTypedValues(Criteria criteria, CriteriaQuery criteriaQuery) throws HibernateException {
            return NO_TYPES;
        }
    }
}