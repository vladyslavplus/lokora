package com.vladyslavplus.lokora.common.http;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
public class RestClientConfiguration {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    RestClient.Builder downstreamRestClientBuilder() {
        return RestClient.builder();
    }
}
