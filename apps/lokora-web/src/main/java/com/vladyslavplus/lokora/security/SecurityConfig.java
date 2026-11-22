package com.vladyslavplus.lokora.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
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
    private final JwtDecoder jwtDecoder;

    public SecurityConfig(LokoraSecurityProperties properties, Clock clock, ObjectMapper objectMapper, JwtDecoder jwtDecoder) {
        this.properties = properties;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        ApiAuthenticationEntryPoint entryPoint = new ApiAuthenticationEntryPoint(clock, objectMapper);
        ApiAccessDeniedHandler deniedHandler = new ApiAccessDeniedHandler(clock, objectMapper);
        http.securityMatcher("/api/**")
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/resources", "/api/resources/*", "/api/reservations/mine")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/resources")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/resources/*/status")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/resources/*")
                        .hasRole("WORKSPACE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/reservations", "/api/reservations/*/cancel", "/api/reservations/*/confirmation")
                        .hasAnyRole("EMPLOYEE", "WORKSPACE_ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain uiSecurityFilterChain(
            HttpSecurity http,
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> tokenResponseClient
    ) throws Exception {
        http.securityMatcher(request -> !request.getRequestURI().startsWith("/api/"))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/css/**", "/error", "/oauth2/**", "/login/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/oauth2/authorization/keycloak")))
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(tokenEndpoint -> tokenEndpoint.accessTokenResponseClient(tokenResponseClient))
                        .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService())))
                .logout(logout -> logout.logoutSuccessUrl("/?logout").deleteCookies("JSESSIONID"))
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
        return new LokoraOidcUserService(jwtDecoder, properties.clientId());
    }

    @Bean
    OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> authorizationCodeTokenResponseClient(
            @Value("${lokora.services.connect-timeout:2s}") Duration connectTimeout,
            @Value("${lokora.services.read-timeout:5s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestClient tokenRestClient = tokenEndpointRestClientBuilder(requestFactory).build();
        return grantRequest -> {
            RestClientAuthorizationCodeTokenResponseClient client = new RestClientAuthorizationCodeTokenResponseClient();
            client.setRestClient(tokenRestClient);
            return client.getTokenResponse(grantRequest);
        };
    }

    static RestClient.Builder tokenEndpointRestClientBuilder(SimpleClientHttpRequestFactory requestFactory) {
        return RestClient.builder()
                .requestFactory(requestFactory)
                .configureMessageConverters(converters -> {
                    converters.addCustomConverter(new FormHttpMessageConverter());
                    converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter());
                })
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler());
    }

    private Collection<GrantedAuthority> jwtAuthorities(Jwt jwt) {
        return KeycloakAuthorityMapper.fromJwt(jwt, properties.clientId());
    }
}
