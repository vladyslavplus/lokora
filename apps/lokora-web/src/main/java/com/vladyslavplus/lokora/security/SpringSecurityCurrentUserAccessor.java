package com.vladyslavplus.lokora.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SpringSecurityCurrentUserAccessor implements CurrentUserAccessor {

    @Override
    public Optional<String> getSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return Optional.ofNullable(jwt.getSubject());
        }
        if (principal instanceof OidcUser oidcUser) {
            return Optional.ofNullable(oidcUser.getSubject());
        }
        return Optional.empty();
    }

    @Override
    public boolean isWorkspaceAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> KeycloakClientRoles.ROLE_WORKSPACE_ADMIN.equals(authority.getAuthority()));
    }
}
