package com.ciff.knowledge.config;

import jakarta.annotation.PostConstruct;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

/**
 * PostgreSQL / PGVector configuration.
 *
 * <p>Uses {@link SimpleDriverDataSource} internally to avoid registering a
 * {@link javax.sql.DataSource} bean that would suppress Spring Boot's MySQL
 * auto-configuration ({@code @ConditionalOnMissingBean(DataSource.class)}).
 *
 * <p>MySQL remains managed by Spring Boot auto-configuration with MyBatis-Plus.
 * PGVector operations go through {@code @Qualifier("pgVectorJdbcTemplate")}.
 */
@Configuration
public class PgVectorConfig {

    @Value("${ciff.pgvector.url}")
    private String url;

    @Value("${ciff.pgvector.username}")
    private String username;

    @Value("${ciff.pgvector.password}")
    private String password;

    @Value("${ciff.pgvector.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    /**
     * Run Flyway migration for PGVector schema on startup.
     * Uses a short-lived datasource; closed immediately after migration.
     */
    @PostConstruct
    public void migrate() {
        SimpleDriverDataSource ds = createDataSource();
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration/pgvector")
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    /**
     * JdbcTemplate for PGVector native SQL operations (vector insert / search).
     */
    @Bean
    @Qualifier("pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate() {
        return new JdbcTemplate(createDataSource());
    }

    private SimpleDriverDataSource createDataSource() {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        try {
            @SuppressWarnings("unchecked")
            Class<? extends java.sql.Driver> driverClass =
                    (Class<? extends java.sql.Driver>) Class.forName(driverClassName);
            ds.setDriverClass(driverClass);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("PGVector driver not found: " + driverClassName, e);
        }
        ds.setUrl(url);
        ds.setUsername(username);
        ds.setPassword(password);
        return ds;
    }
}