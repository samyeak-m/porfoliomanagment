package com.stockmanagment.porfoliomanagment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.stockmanagment.porfoliomanagment.repository.portfolio",
        entityManagerFactoryRef = "portfolioEntityManagerFactory",
        transactionManagerRef = "portfolioTransactionManager"
)
public class PortfolioDatabaseConfig {

    @Bean(name = "portfolioDataSource")
    public DataSource portfolioDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/portfolio")
                .username("root")
                .password("")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean(name = "portfolioEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean portfolioEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("portfolioDataSource") DataSource dataSource) {
        Map<String, String> nepseJpaProperties = new HashMap<>();
        nepseJpaProperties.put("hibernate.hbm2ddl.auto", "validate");
        nepseJpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");

        return builder
                .dataSource(dataSource)
                .packages("com.stockmanagment.porfoliomanagment.model.portfolio")
                .persistenceUnit("portfolio")
                .build();
    }

    @Bean(name = "portfolioTransactionManager")
    public PlatformTransactionManager portfolioTransactionManager(
            @Qualifier("portfolioEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
