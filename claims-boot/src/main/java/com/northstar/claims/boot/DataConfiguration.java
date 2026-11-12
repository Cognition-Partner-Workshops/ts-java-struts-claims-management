package com.northstar.claims.boot;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.hsqldb.server.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

@Configuration
public class DataConfiguration {
    @Bean
    Object databaseBootstrap(@Value("${claims.db.url}") String url,
                             @Value("${claims.db.reset}") boolean reset,
                             ClaimsConfig config) throws Exception {
        if (!url.startsWith("jdbc:hsqldb:file:")) {
            throw new IllegalArgumentException("claims.db.url must be an HSQL file URL");
        }
        var parent = new java.io.File(url.substring("jdbc:hsqldb:file:".length())).getParentFile();
        if (parent != null) {
            java.nio.file.Files.createDirectories(parent.toPath());
        }
        DatabaseBootstrap.bootstrap(url, reset, config);
        return new Object();
    }

    @Bean(destroyMethod = "close")
    @DependsOn("databaseBootstrap")
    HikariDataSource dataSource(@Value("${claims.db.url}") String url, ClaimsConfig config) {
        HikariDataSource source = new HikariDataSource();
        source.setJdbcUrl(url);
        source.setUsername(config.get("db.user"));
        source.setPassword(config.get("db.password"));
        source.setAutoCommit(true);
        return source;
    }

    @Bean
    DataSourceTransactionManager transactionManager(DataSource source) {
        return new DataSourceTransactionManager(source);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn("dataSource")
    @ConditionalOnProperty(name = "claims.db.server.enabled", havingValue = "true")
    Server databaseServer(@Value("${claims.db.url}") String url,
                          @Value("${claims.db.server.port}") int port) {
        Server server = new Server();
        server.setAddress("127.0.0.1");
        server.setPort(port);
        server.setDatabaseName(0, "northstar");
        server.setDatabasePath(0, url.substring("jdbc:hsqldb:".length()));
        server.setSilent(true);
        server.setNoSystemExit(true);
        return server;
    }
}
