package com.vladyslavplus.lokora.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class DownstreamAccessTokenProvider {
    private final OAuth2AuthorizedClientService authorizedClientService;

    public DownstreamAccessTokenProvider(OAuth2AuthorizedClientService authorizedClientService) {
        this.authorizedClientService = authorizedClientService;
    }

    public String currentAccessToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getTokenValue();
        }
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                    oauth2Authentication.getAuthorizedClientRegistrationId(),
                    oauth2Authentication.getName()
            );
            if (client != null) {
                return client.getAccessToken().getTokenValue();
            }
        }
        throw new IllegalStateException("An authenticated user access token is required for service calls");
    }
}
