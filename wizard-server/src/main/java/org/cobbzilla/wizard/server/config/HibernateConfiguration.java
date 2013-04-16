package org.cobbzilla.wizard.server.config;

import lombok.Getter;
import lombok.Setter;

public class HibernateConfiguration {

    @Getter @Setter private String[] entityPackages;

    @Getter @Setter private String dialect;
    @Getter @Setter private boolean showSql;
    @Getter @Setter private String hbm2ddlAuto;
    @Getter @Setter private String validationMode;

}
