package com.vladyslavplus.lokora.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakAuthorityConverterTest {

    private static final String CLIENT_ID = "lokora-app";

    @Test
    void mapsEmployeeClientRoleToEmployeeAuthority() {
        Jwt jwt = jwtWithRoles(List.of("EMPLOYEE"));

        Collection<GrantedAuthority> authorities = KeycloakAuthorityMapper.fromJwt(jwt, CLIENT_ID);

        assertThat(authorities).containsExactly(new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_EMPLOYEE));
    }

    @Test
    void mapsWorkspaceAdminClientRoleToAdminAndEmployeeAuthorities() {
        Jwt jwt = jwtWithRoles(List.of("WORKSPACE_ADMIN"));

        Collection<GrantedAuthority> authorities = KeycloakAuthorityMapper.fromJwt(jwt, CLIENT_ID);

        assertThat(authorities).containsExactlyInAnyOrder(
                new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_WORKSPACE_ADMIN),
                new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_EMPLOYEE)
        );
    }

    @Test
    void tokenWithoutLokoraRolesProducesNoApplicationAuthorities() {
        Jwt jwt = jwtWithRoles(List.of());

        Collection<GrantedAuthority> authorities = KeycloakAuthorityMapper.fromJwt(jwt, CLIENT_ID);

        assertThat(authorities).isEmpty();
    }

    @Test
    void unrelatedClientRolesAreIgnored() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        "other-client", Map.of("roles", List.of("WORKSPACE_ADMIN")),
                        CLIENT_ID, Map.of("roles", List.of("EMPLOYEE"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        Collection<GrantedAuthority> authorities = KeycloakAuthorityMapper.fromJwt(jwt, CLIENT_ID);

        assertThat(authorities).containsExactly(new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_EMPLOYEE));
    }

    @Test
    void oidcSessionClaimsUseSameMappingAsJwt() {
        Map<String, Object> claims = Map.of(
                "sub", "user-1",
                "resource_access", Map.of(
                        CLIENT_ID, Map.of("roles", List.of("WORKSPACE_ADMIN"))
                )
        );

        Collection<GrantedAuthority> authorities = KeycloakAuthorityMapper.fromClaims(claims, CLIENT_ID);

        assertThat(authorities).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        KeycloakClientRoles.ROLE_WORKSPACE_ADMIN,
                        KeycloakClientRoles.ROLE_EMPLOYEE
                );
    }

    private Jwt jwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        CLIENT_ID, Map.of("roles", roles)
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
