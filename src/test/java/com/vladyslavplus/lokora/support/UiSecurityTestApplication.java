package com.vladyslavplus.lokora.support;

import com.vladyslavplus.lokora.common.api.GlobalExceptionHandler;
import com.vladyslavplus.lokora.config.ClockConfig;
import com.vladyslavplus.lokora.security.SecurityConfig;
import com.vladyslavplus.lokora.ui.HomeController;
import com.vladyslavplus.lokora.ui.ResourceUiController;
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
        HomeController.class,
        ResourceUiController.class,
        GlobalExceptionHandler.class,
        ClockConfig.class
})
public class UiSecurityTestApplication {
}
