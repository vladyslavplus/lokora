package com.vladyslavplus.lokora.support;

import com.vladyslavplus.lokora.common.api.GlobalExceptionHandler;
import com.vladyslavplus.lokora.config.ClockConfig;
import com.vladyslavplus.lokora.reservation.ReservationController;
import com.vladyslavplus.lokora.security.SecurityConfig;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        QuartzAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        ReservationController.class,
        GlobalExceptionHandler.class,
        ClockConfig.class
})
public class ApiSecurityTestApplication {
}
