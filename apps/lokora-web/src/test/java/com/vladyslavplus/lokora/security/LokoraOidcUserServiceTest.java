package com.vladyslavplus.lokora.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LokoraOidcUserServiceTest {

    private static final String CLIENT_ID = "lokora-app";

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @Mock
    private JwtDecoder jwtDecoder;

    @Test
    void mapsClientRolesFromAccessTokenNotFromIdTokenClaims() {
        OidcUser delegateUser = new DefaultOidcUser(
                List.of(),
                idTokenWithoutRoles(),
                userInfoWithoutRoles(),
                "sub"
        );
        when(delegate.loadUser(any())).thenReturn(delegateUser);
        when(jwtDecoder.decode("access-token-value")).thenReturn(accessTokenWithEmployeeRole());

        LokoraOidcUserService service = new LokoraOidcUserService(delegate, jwtDecoder, CLIENT_ID);
        OidcUser mappedUser = service.loadUser(oidcUserRequest());

        assertThat(mappedUser.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly(KeycloakClientRoles.ROLE_EMPLOYEE);
    }

    @Test
    void workspaceAdminAccessTokenIncludesAdminAndEmployeeAuthorities() {
        OidcUser delegateUser = new DefaultOidcUser(
                List.of(),
                idTokenWithoutRoles(),
                userInfoWithoutRoles(),
                "sub"
        );
        when(delegate.loadUser(any())).thenReturn(delegateUser);
        when(jwtDecoder.decode("access-token-value")).thenReturn(accessTokenWithAdminRole());

        LokoraOidcUserService service = new LokoraOidcUserService(delegate, jwtDecoder, CLIENT_ID);
        OidcUser mappedUser = service.loadUser(oidcUserRequest());

        assertThat(mappedUser.getAuthorities()).extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        KeycloakClientRoles.ROLE_WORKSPACE_ADMIN,
                        KeycloakClientRoles.ROLE_EMPLOYEE
                );
    }

    private OidcUserRequest oidcUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId(CLIENT_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/keycloak")
                .authorizationUri("http://localhost:8180/realms/lokora/protocol/openid-connect/auth")
                .tokenUri("http://localhost:8180/realms/lokora/protocol/openid-connect/token")
                .jwkSetUri("http://localhost:8180/realms/lokora/protocol/openid-connect/certs")
                .issuerUri("http://localhost:8180/realms/lokora")
                .userInfoUri("http://localhost:8180/realms/lokora/protocol/openid-connect/userinfo")
                .userNameAttributeName("sub")
                .clientName("keycloak")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600)
        );
        return new OidcUserRequest(registration, accessToken, idTokenWithoutRoles());
    }

    private OidcIdToken idTokenWithoutRoles() {
        return new OidcIdToken(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("sub", "user-1")
        );
    }

    private OidcUserInfo userInfoWithoutRoles() {
        return new OidcUserInfo(Map.of("sub", "user-1"));
    }

    private Jwt accessTokenWithEmployeeRole() {
        return Jwt.withTokenValue("access-token-value")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        CLIENT_ID, Map.of("roles", List.of("EMPLOYEE"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    private Jwt accessTokenWithAdminRole() {
        return Jwt.withTokenValue("access-token-value")
                .header("alg", "none")
                .claim("sub", "user-1")
                .claim("resource_access", Map.of(
                        CLIENT_ID, Map.of("roles", List.of("WORKSPACE_ADMIN"))
                ))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
