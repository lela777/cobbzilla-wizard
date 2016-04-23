package org.cobbzilla.wizard.model.json;

import org.hibernate.dialect.PostgreSQL9Dialect;

import java.sql.Types;

/**
 * see: https://stackoverflow.com/a/16049086/1251543
 * Wrap default PostgreSQL9Dialect with 'json' type.
 * @author timfulmer
 */
public class JsonPostgreSQLDialect extends PostgreSQL9Dialect {

    public JsonPostgreSQLDialect() {
        super();
        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
    }
}