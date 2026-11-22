package com.vladyslavplus.lokora.security;

import com.vladyslavplus.lokora.common.api.ApiErrorResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(LokoraSecurityProperties.class)
public class SecurityConfig {
    private final LokoraSecurityProperties properties;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public SecurityConfig(LokoraSecurityProperties properties, Clock clock, ObjectMapper objectMapper) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(clock, objectMapper);
        ApiAccessDeniedHandler deniedHandler = new ApiAccessDeniedHandler(clock, objectMapper);
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/resources", "/api/resources/*")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/resources")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/*/status")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/internal/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/resources/internal/resources/*/reservation-availability")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::jwtAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> jwtAuthorities(Jwt jwt) {
        return KeycloakAuthorityMapper.fromJwt(jwt, properties.clientId());
    }
}
