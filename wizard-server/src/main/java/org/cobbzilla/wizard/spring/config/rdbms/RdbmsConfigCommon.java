package org.cobbzilla.wizard.spring.config.rdbms;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.PoisonProxy;
import org.cobbzilla.wizard.model.EncryptedTypes;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HibernateConfiguration;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.jasypt.encryption.pbe.PBEStringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.hibernate4.encryptor.HibernatePBEStringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.orm.hibernate4.LocalSessionFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class RdbmsConfigCommon {

    @SuppressWarnings("SpringJavaAutowiringInspection")
    @Autowired protected HasDatabaseConfiguration configuration;
    protected HasDatabaseConfiguration configuration () { return configuration; }

    public DataSource dataSource() {
        final DatabaseConfiguration dbConfiguration = configuration().getDatabase();
        final DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName(dbConfiguration.getDriver());
        ds.setUrl(dbConfiguration.getUrl());
        ds.setUsername(dbConfiguration.getUser());
        ds.setPassword(dbConfiguration.getPassword());
        return ds;
    }

    public HibernateTransactionManager transactionManager(SessionFactory sessionFactory) {
        final HibernateTransactionManager htm = new HibernateTransactionManager();
        htm.setSessionFactory(sessionFactory);
        return htm;
    }

    public HibernateTemplate hibernateTemplate(SessionFactory sessionFactory) {
        final HibernateTemplate hibernateTemplate = new HibernateTemplate(sessionFactory);
        return hibernateTemplate;
    }

    public LocalSessionFactoryBean sessionFactory() {
        final LocalSessionFactoryBean factory = new LocalSessionFactoryBean();
        factory.setNamingStrategy(ImprovedNamingStrategy.INSTANCE);
        factory.setDataSource(dataSource());
        factory.setHibernateProperties(hibernateProperties());
        factory.setPackagesToScan(configuration().getDatabase().getHibernate().getEntityPackages());
        return factory;
    }

    public Properties hibernateProperties() {
        final HibernateConfiguration hibernateConfiguration = configuration().getDatabase().getHibernate();
        final Properties properties = new Properties();
        properties.put("hibernate.dialect", hibernateConfiguration.getDialect());
        properties.put("hibernate.show_sql", hibernateConfiguration.isShowSql());
        properties.put("hibernate.hbm2ddl.auto", hibernateConfiguration.getHbm2ddlAuto());
        properties.put("hibernate.validator.apply_to_ddl", hibernateConfiguration.isApplyValidatorToDDL());
        properties.put("javax.persistence.verification.mode", hibernateConfiguration.getValidationMode());
        return properties;
    }

    public static final int MIN_KEY_LENGTH = 15;
    @Bean public PBEStringEncryptor strongEncryptor () {

        if (!configuration.getDatabase().isEncryptionEnabled()) {
            log.warn("strongEncryptor: encryption is disabled, will not work!");
            return PoisonProxy.wrap(PBEStringEncryptor.class);
        }

        final String key = configuration.getDatabase().getEncryptionKey();
        if (empty(key) || key.length() < MIN_KEY_LENGTH) die("strongEncryptor: encryption was enabled, but key was too short (min length "+MIN_KEY_LENGTH+"): '"+key+"'");

        final PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(key);
        encryptor.setAlgorithm("PBEWithMD5AndTripleDES");
        encryptor.setPoolSize(configuration.getDatabase().getEncryptorPoolSize());
        return encryptor;
    }

    @Bean public HibernatePBEStringEncryptor hibernateEncryptor() {
        final HibernatePBEStringEncryptor encryptor = new HibernatePBEStringEncryptor();
        encryptor.setEncryptor(strongEncryptor());
        encryptor.setRegisteredName(EncryptedTypes.STRING_ENCRYPTOR_NAME);
        return encryptor;
    }

    @Bean public HibernatePBEStringEncryptor hibernateIntegerEncryptor() {
        final HibernatePBEStringEncryptor encryptor = new HibernatePBEStringEncryptor();
        encryptor.setEncryptor(strongEncryptor());
        encryptor.setRegisteredName(EncryptedTypes.INTEGER_ENCRYPTOR_NAME);
        return encryptor;
    }

    @Bean public HibernatePBEStringEncryptor hibernateLongEncryptor() {
        final HibernatePBEStringEncryptor encryptor = new HibernatePBEStringEncryptor();
        encryptor.setEncryptor(strongEncryptor());
        encryptor.setRegisteredName(EncryptedTypes.LONG_ENCRYPTOR_NAME);
        return encryptor;
    }
}
