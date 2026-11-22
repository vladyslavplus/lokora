package com.vladyslavplus.lokora.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@TestConfiguration
public class OAuth2TestClientConfiguration {

    @Bean
    @Primary
    ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("lokora-app")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email")
                .authorizationUri("http://127.0.0.1:9/realms/lokora/protocol/openid-connect/auth")
                .tokenUri("http://127.0.0.1:9/realms/lokora/protocol/openid-connect/token")
                .jwkSetUri("http://127.0.0.1:9/realms/lokora/protocol/openid-connect/certs")
                .issuerUri("http://127.0.0.1:9/realms/lokora")
                .userInfoUri("http://127.0.0.1:9/realms/lokora/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .clientName("keycloak")
                .build();
        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder() {
        return token -> {
            throw new JwtException("JWT decoding is not used in security MockMvc tests");
        };
    }
}
