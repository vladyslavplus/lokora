package com.vladyslavplus.lokora.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(LokoraSecurityProperties.class)
public class SecurityConfig {

    private final LokoraSecurityProperties securityProperties;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final JwtDecoder jwtDecoder;

    public SecurityConfig(
            LokoraSecurityProperties securityProperties,
            Clock clock,
            ObjectMapper objectMapper,
            JwtDecoder jwtDecoder
    ) {
        this.securityProperties = securityProperties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiAuthenticationEntryPoint authenticationEntryPoint =
                new ApiAuthenticationEntryPoint(clock, objectMapper);
        ApiAccessDeniedHandler accessDeniedHandler = new ApiAccessDeniedHandler(clock, objectMapper);

        http.securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/resources", "/api/resources/*")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/*/status")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/resources")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/mine")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/cancel")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations/*/confirmation")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(request -> !request.getRequestURI().startsWith("/api/"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers("/resources/**").authenticated()
                        .requestMatchers("/reservations/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak"))
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService()))
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/?logout")
                        .deleteCookies("JSESSIONID")
                )
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        return http.build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::jwtAuthorities);
        return converter;
    }

    @Bean
    OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        return new LokoraOidcUserService(
                jwtDecoder,
                securityProperties.clientId()
        );
    }

    private Collection<GrantedAuthority> jwtAuthorities(Jwt jwt) {
        return KeycloakAuthorityMapper.fromJwt(jwt, securityProperties.clientId());
    }
}
