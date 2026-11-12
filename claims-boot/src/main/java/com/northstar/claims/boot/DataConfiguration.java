package com.northstar.claims.boot;

import com.northstar.claims.boot.util.ClaimsConfig;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class DataConfiguration {
    @Bean
    DataSource dataSource() {
        return new DriverManagerDataSource("jdbc:hsqldb:mem:claimsboot",
                ClaimsConfig.get("db.user"), ClaimsConfig.get("db.password"));
    }
}
