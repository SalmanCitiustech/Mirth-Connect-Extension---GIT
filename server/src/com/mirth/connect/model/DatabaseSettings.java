/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * 
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL license a copy of which has
 * been included with this distribution in the LICENSE.txt file.
 */

package com.mirth.connect.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("databaseSettings")
public class DatabaseSettings extends AbstractSettings implements Serializable, Auditable {
    private static final long serialVersionUID = 1L;

    private static final String DATABASE = "database";
    private static final String DATABASE_URL = "database.url";
    private static final String DATABASE_DRIVER = "database.driver";
    private static final String DATABASE_USERNAME = "database.username";
    private static final String DATABASE_PASSWORD = "database.password";
    private static final String DATABASE_MAX_CONNECTIONS = "database.max-connections";
    private static final String DATABASE_POOL = "database.pool";
    private static final String DATABASE_JDBC4 = "database.jdbc4";
    private static final String DATABASE_TEST_QUERY = "database.test-query";
    private static final String DATABASE_TEST_IDLE_TIME = "database.test-idle-time";

    private static final String DIR_BASE = "dir.base";

    private static Map<String, String> databaseDriverMap = null;
    private static Map<String, Boolean> databaseJdbc4Map = null;
    private static Map<String, String> databaseTestQueryMap = null;

    static {
        databaseDriverMap = new HashMap<String, String>();
        databaseDriverMap.put("derby", "org.apache.derby.jdbc.EmbeddedDriver");
        databaseDriverMap.put("mysql", "com.mysql.jdbc.Driver");
        databaseDriverMap.put("oracle", "oracle.jdbc.OracleDriver");
        databaseDriverMap.put("postgres", "org.postgresql.Driver");
        databaseDriverMap.put("sqlserver", "net.sourceforge.jtds.jdbc.Driver");

        databaseJdbc4Map = new HashMap<String, Boolean>();
        databaseJdbc4Map.put("derby", true);
        databaseJdbc4Map.put("mysql", true);
        databaseJdbc4Map.put("oracle", true);
        databaseJdbc4Map.put("postgres", true);
        // JTDS does not support JDBC 4.0 operations
        databaseJdbc4Map.put("sqlserver", false);

        databaseTestQueryMap = new HashMap<String, String>();
        databaseTestQueryMap.put("derby", "SELECT 1");
        databaseTestQueryMap.put("mysql", "SELECT 1");
        databaseTestQueryMap.put("oracle", "SELECT 1 FROM DUAL");
        databaseTestQueryMap.put("postgres", "SELECT 1");
        databaseTestQueryMap.put("sqlserver", "SELECT 1");
    }

    private String database;
    private String databaseUrl;
    private String databaseDriver;
    private String databaseUsername;
    private String databasePassword;
    private Integer databaseMaxConnections;
    private String databasePool;
    private Integer databaseTestIdleTime;
    private String dirBase;

    public DatabaseSettings() {

    }

    public DatabaseSettings(Properties properties) {
        setProperties(properties);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    public String getDatabaseDriver() {
        return databaseDriver;
    }

    public void setDatabaseDriver(String databaseDriver) {
        this.databaseDriver = databaseDriver;
    }

    public String getDatabaseUsername() {
        return databaseUsername;
    }

    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    public String getDatabasePassword() {
        return databasePassword;
    }

    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    public Integer getDatabaseMaxConnections() {
        return databaseMaxConnections;
    }

    public void setDatabaseMaxConnections(Integer databaseMaxConnections) {
        this.databaseMaxConnections = databaseMaxConnections;
    }

    public String getDatabasePool() {
        return databasePool;
    }

    public void setDatabasePool(String databasePool) {
        this.databasePool = databasePool;
    }

    public Integer getDatabaseTestIdleTime() {
        return databaseTestIdleTime;
    }

    public void setDatabaseTestIdleTime(Integer databaseTestIdleTime) {
        this.databaseTestIdleTime = databaseTestIdleTime;
    }

    public String getDirBase() {
        return dirBase;
    }

    public void setDirBase(String dirBase) {
        this.dirBase = dirBase;
    }

    private String getMappedDatabaseDriver() {
        if (StringUtils.isEmpty(databaseDriver)) {
            return MapUtils.getString(databaseDriverMap, getDatabase());
        } else {
            return databaseDriver;
        }
    }

    private Boolean getMappedJdbc4() {
        return MapUtils.getBoolean(databaseJdbc4Map, getDatabase());
    }

    private String getMappedTestQuery() {
        return MapUtils.getString(databaseTestQueryMap, getDatabase());
    }

    @Override
    public void setProperties(Properties properties) {
        setDatabase(properties.getProperty(DATABASE));
        setDatabaseUrl(properties.getProperty(DATABASE_URL));
        setDatabaseDriver(properties.getProperty(DATABASE_DRIVER));
        setDatabaseUsername(properties.getProperty(DATABASE_USERNAME));
        setDatabasePassword(properties.getProperty(DATABASE_PASSWORD));
        setDatabaseMaxConnections(Integer.parseInt(properties.getProperty(DATABASE_MAX_CONNECTIONS)));
        setDatabasePool(properties.getProperty(DATABASE_POOL));
        setDatabaseTestIdleTime(Integer.parseInt(properties.getProperty(DATABASE_TEST_IDLE_TIME, "10000")));
        setDirBase(properties.getProperty(DIR_BASE));
    }

    @Override
    public Properties getProperties() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();

        if (getDirBase() != null) {
            configuration.setProperty(DIR_BASE, getDirBase());
        }

        if (getDatabase() != null) {
            configuration.setProperty(DATABASE, getDatabase());
        }

        if (getDatabaseUrl() != null) {
            configuration.setProperty(DATABASE_URL, getDatabaseUrl());
        }

        if (getMappedDatabaseDriver() != null) {
            configuration.setProperty(DATABASE_DRIVER, getMappedDatabaseDriver());
        }

        if (getDatabasePool() != null) {
            configuration.setProperty(DATABASE_POOL, getDatabasePool());
        }

        if (getMappedJdbc4() != null) {
            configuration.setProperty(DATABASE_JDBC4, getMappedJdbc4());
        }

        if (getMappedTestQuery() != null) {
            configuration.setProperty(DATABASE_TEST_QUERY, getMappedTestQuery());
        }

        /*
         * MIRTH-1749: in case someone comments out the username and password properties
         */
        if (getDatabaseUsername() != null) {
            configuration.setProperty(DATABASE_USERNAME, getDatabaseUsername());
        } else {
            configuration.setProperty(DATABASE_USERNAME, StringUtils.EMPTY);
        }

        if (getDatabasePassword() != null) {
            configuration.setProperty(DATABASE_PASSWORD, getDatabasePassword());
        } else {
            configuration.setProperty(DATABASE_PASSWORD, StringUtils.EMPTY);
        }

        if (getDatabaseMaxConnections() != null) {
            configuration.setProperty(DATABASE_MAX_CONNECTIONS, getDatabaseMaxConnections().toString());
        } else {
            configuration.setProperty(DATABASE_MAX_CONNECTIONS, StringUtils.EMPTY);
        }

        if (getDatabaseTestIdleTime() != null) {
            configuration.setProperty(DATABASE_TEST_IDLE_TIME, getDatabaseTestIdleTime().toString());
        } else {
            configuration.setProperty(DATABASE_TEST_IDLE_TIME, "10000");
        }

        return ConfigurationConverter.getProperties(configuration);
    }

    @Override
    public String toAuditString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
    }

}
