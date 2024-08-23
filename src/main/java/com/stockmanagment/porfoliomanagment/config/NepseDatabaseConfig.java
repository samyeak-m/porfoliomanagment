package com.stockmanagment.porfoliomanagment.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.stockmanagment.porfoliomanagment.repository.nepse",
        entityManagerFactoryRef = "nepseEntityManagerFactory",
        transactionManagerRef = "nepseTransactionManager"
)
public class NepseDatabaseConfig {

    @Bean(name = "nepseDataSource")
    @Qualifier("nepseDataSource")
    public DataSource nepseDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/nepse")
                .username("root")
                .password("")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder() {
        return new EntityManagerFactoryBuilder(new HibernateJpaVendorAdapter(), new HashMap<>(), null);
    }

    @Bean(name = "nepseEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean nepseEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("nepseDataSource") DataSource dataSource) {
        Map<String, String> nepseJpaProperties = new HashMap<>();
        nepseJpaProperties.put("hibernate.hbm2ddl.auto", "update");
        nepseJpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");

        return builder
                .dataSource(dataSource)
                .packages("com.stockmanagment.porfoliomanagment.model.nepse") // Fixed package path
                .persistenceUnit("nepse")
                .build();
    }

    @Bean(name = "nepseTransactionManager")
    public PlatformTransactionManager nepseTransactionManager(
            @Qualifier("nepseEntityManagerFactory") LocalContainerEntityManagerFactoryBean nepseEntityManagerFactory) {
        return new JpaTransactionManager(nepseEntityManagerFactory.getObject());
    }
}
