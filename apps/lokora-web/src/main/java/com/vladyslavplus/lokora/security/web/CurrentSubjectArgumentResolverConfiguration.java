package com.vladyslavplus.lokora.security.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class CurrentSubjectArgumentResolverConfiguration implements WebMvcConfigurer {

    private final CurrentSubjectArgumentResolver currentSubjectArgumentResolver;

    public CurrentSubjectArgumentResolverConfiguration(CurrentSubjectArgumentResolver currentSubjectArgumentResolver) {
        this.currentSubjectArgumentResolver = currentSubjectArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentSubjectArgumentResolver);
    }
}
