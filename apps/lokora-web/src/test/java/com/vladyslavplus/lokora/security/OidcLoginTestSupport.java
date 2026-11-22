package com.vladyslavplus.lokora.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OidcLoginTestSupport {

    private static final String CLIENT_ID = "lokora-app";

    private OidcLoginTestSupport() {
    }

    public static RequestPostProcessor oidcLoginWithClientRole(String clientRole) {
        Jwt accessToken = Jwt.withTokenValue("test-access-token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        CLIENT_ID, Map.of("roles", List.of(clientRole))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        var authorities = KeycloakAuthorityMapper.fromJwt(accessToken, CLIENT_ID).stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getAuthority()))
                .toArray(SimpleGrantedAuthority[]::new);
        return SecurityMockMvcRequestPostProcessors.oidcLogin()
                .idToken(token -> token.claim("sub", "user-1"))
                .authorities(authorities);
    }
}
