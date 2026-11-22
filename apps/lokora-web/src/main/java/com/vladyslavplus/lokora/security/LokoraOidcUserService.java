package com.vladyslavplus.lokora.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

public class LokoraOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;
    private final JwtDecoder jwtDecoder;
    private final String clientId;

    public LokoraOidcUserService(
            JwtDecoder jwtDecoder,
            String clientId
    ) {
        this.delegate = new OidcUserService();
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
    }

    LokoraOidcUserService(
            OAuth2UserService<OidcUserRequest, OidcUser> delegate,
            JwtDecoder jwtDecoder,
            String clientId
    ) {
        this.delegate = delegate;
        this.jwtDecoder = jwtDecoder;
        this.clientId = clientId;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        Jwt accessToken = jwtDecoder.decode(userRequest.getAccessToken().getTokenValue());
        var authorities = KeycloakAuthorityMapper.fromJwt(accessToken, clientId);
        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "sub"
        );
    }
}
