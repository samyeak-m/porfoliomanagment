package com.stockmanagment.porfoliomanagment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.stockmanagment.porfoliomanagment.repository.portfolio",
        entityManagerFactoryRef = "portfolioEntityManagerFactory",
        transactionManagerRef = "portfolioTransactionManager"

)
public class PortfolioDatabaseConfig {

    @Bean(name = "portfolioDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.portfolio")
    public DataSource dataSource(){
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "portfolioEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean portfolioEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("portfolioDataSource") DataSource dataSource) {
        HashMap<String, Object> portfolioJpaProperties = new HashMap<>();
        portfolioJpaProperties.put("hibernate.hbm2ddl.auto", "update");

        return builder
                .dataSource(dataSource)
                .properties(portfolioJpaProperties)
                .packages("com.stockmanagment.porfoliomanagment.model.portfolio") // Fixed package path
                .persistenceUnit("portfolio")
                .build();
    }

    @Bean(name = "portfolioTransactionManager")
    public PlatformTransactionManager portfolioTransactionManager(
            @Qualifier("portfolioEntityManagerFactory") EntityManagerFactory portfolioEntityManagerFactory) {
        return new JpaTransactionManager(portfolioEntityManagerFactory);
    }
}
