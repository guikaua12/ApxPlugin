package me.approximations.apxPlugin.persistence.jpa.config.impl;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import me.approximations.apxPlugin.persistence.jpa.config.PersistenceUnitConfig;

import javax.persistence.spi.PersistenceUnitInfo;
import javax.sql.DataSource;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Setter
public class HikariPersistenceUnitConfig extends PersistenceUnitConfig implements PersistenceUnitInfo {
    public static final String ADDRESS_FORMAT = "jdbc:mysql://%s/%s?serverTimezone=UTC";

    private static final int MAXIMUM_POOL_SIZE = (Runtime.getRuntime().availableProcessors() * 2) + 1;
    private static final int MINIMUM_IDLE = Math.min(MAXIMUM_POOL_SIZE, 10);

    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long LEAK_DETECTION_THRESHOLD = TimeUnit.SECONDS.toMillis(10);

    private final HikariDataSource dataSource = new HikariDataSource();

    public HikariPersistenceUnitConfig(String persistenceUnitName, String address, String username, String password, String database, Class<? extends Driver> jdbcDriver, boolean showSql) {
        super(persistenceUnitName, address, username, password, database, jdbcDriver, showSql);

        dataSource.setJdbcUrl(getFormattedAddress());
        dataSource.setDriverClassName(jdbcDriver.getName());

        dataSource.setUsername(username);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        dataSource.setMinimumIdle(MINIMUM_IDLE);

        dataSource.setMaxLifetime(MAX_LIFETIME);
        dataSource.setConnectionTimeout(CONNECTION_TIMEOUT);
        dataSource.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD);

        dataSource.addDataSourceProperty("useUnicode", true);
        dataSource.addDataSourceProperty("characterEncoding", "utf8");

        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("useLocalSessionState", "true");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        dataSource.addDataSourceProperty("cacheResultSetMetadata", "true");
        dataSource.addDataSourceProperty("cacheServerConfiguration", "true");
        dataSource.addDataSourceProperty("elideSetAutoCommits", "true");
        dataSource.addDataSourceProperty("maintainTimeStats", "false");
        dataSource.addDataSourceProperty("alwaysSendSetIsolation", "false");
        dataSource.addDataSourceProperty("cacheCallableStmts", "true");

        dataSource.addDataSourceProperty("socketTimeout", String.valueOf(TimeUnit.SECONDS.toMillis(30)));
    }

    private String getFormattedAddress() {
        return String.format(ADDRESS_FORMAT, address, database);
    }

    @Override
    public String getPersistenceUnitName() {
        return "jpa-example";
    }

    @Override
    public DataSource getNonJtaDataSource() {
        return dataSource;
    }

    @Override
    public List<String> getManagedClassNames() {
        List<String> classNames = new ArrayList<>();
        classNames.add("me.approximations.jpaExample.model.People");
        classNames.add("me.approximations.jpaExample.model.Permission");
        return classNames;
    }

    @Override
    public Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", getFormattedAddress());
        properties.setProperty("hibernate.connection.username", username);
        properties.setProperty("hibernate.connection.password", password);
        properties.setProperty("hibernate.connection.driver_class", jdbcDriver.getName());
        // properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        properties.setProperty("hibernate.show_sql", String.valueOf(showSql));
        properties.setProperty("hibernate.hbm2ddl.auto", "update");
        return properties;
    }
}