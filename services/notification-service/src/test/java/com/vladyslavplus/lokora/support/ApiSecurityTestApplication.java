package com.vladyslavplus.lokora.support;

import com.vladyslavplus.lokora.common.api.GlobalExceptionHandler;
import com.vladyslavplus.lokora.config.ClockConfig;
import com.vladyslavplus.lokora.notification.NotificationController;
import com.vladyslavplus.lokora.security.SecurityConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        NotificationController.class,
        GlobalExceptionHandler.class,
        ClockConfig.class
})
public class ApiSecurityTestApplication {
}
