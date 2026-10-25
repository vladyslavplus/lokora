package com.vladyslavplus.lokora.support;

import com.vladyslavplus.lokora.config.ClockConfig;
import com.vladyslavplus.lokora.security.SecurityConfig;
import com.vladyslavplus.lokora.ui.ReservationUiController;
import com.vladyslavplus.lokora.ui.ResourceReservationUiController;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.liquibase.autoconfigure.LiquibaseAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        LiquibaseAutoConfiguration.class,
        OAuth2ClientAutoConfiguration.class
})
@Import({
        SecurityConfig.class,
        ReservationUiController.class,
        ResourceReservationUiController.class,
        ClockConfig.class
})
public class ReservationUiTestApplication {
}
