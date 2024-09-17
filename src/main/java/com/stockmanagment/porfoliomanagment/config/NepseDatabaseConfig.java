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
        basePackages = "com.stockmanagment.porfoliomanagment.repository.nepse",
        entityManagerFactoryRef = "nepseEntityManagerFactory",
        transactionManagerRef = "nepseTransactionManager"

)
public class NepseDatabaseConfig {

    @Primary
    @Bean(name = "nepseDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.nepse")
    public DataSource dataSource(){
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "nepseEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nepseEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("nepseDataSource") DataSource dataSource) {
        HashMap<String, Object> nepseJpaProperties = new HashMap<>();
        nepseJpaProperties.put("hibernate.hbm2ddl.auto", "update");

        return builder
                .dataSource(dataSource)
                .properties(nepseJpaProperties)
                .packages("com.stockmanagment.porfoliomanagment.model.nepse") // Fixed package path
                .persistenceUnit("nepse")
                .build();
    }

    @Primary
    @Bean(name = "nepseTransactionManager")
    public PlatformTransactionManager nepseTransactionManager(
            @Qualifier("nepseEntityManagerFactory") EntityManagerFactory nepseEntityManagerFactory) {
        return new JpaTransactionManager(nepseEntityManagerFactory);
    }
}
