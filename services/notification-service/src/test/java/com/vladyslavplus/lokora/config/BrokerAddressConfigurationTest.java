package com.vladyslavplus.lokora.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class BrokerAddressConfigurationTest {

    private MockEnvironment environment() throws IOException {
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addLast(new PropertiesPropertySource(
                "application", PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"))
        ));
        return environment;
    }

    @Test
    void brokerHost_withoutEnvironmentVariable_defaultsToLocalhostForIdeRuns() throws IOException {
        MockEnvironment environment = environment();

        assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("localhost");
        assertThat(environment.getProperty("spring.rabbitmq.port")).isEqualTo("5672");
    }

    @Test
    void brokerHost_withEnvironmentVariable_usesComposeServiceName() throws IOException {
        MockEnvironment environment = environment();
        environment.setProperty("RABBITMQ_HOST", "rabbitmq");

        assertThat(environment.getProperty("spring.rabbitmq.host")).isEqualTo("rabbitmq");
    }
}
