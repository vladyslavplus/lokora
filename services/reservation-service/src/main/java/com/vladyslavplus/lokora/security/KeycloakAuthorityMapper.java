package com.vladyslavplus.lokora.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeycloakAuthorityMapper {

    private KeycloakAuthorityMapper() {
    }

    public static Collection<GrantedAuthority> fromJwt(Jwt jwt, String clientId) {
        return fromClaims(jwt.getClaims(), clientId);
    }

    public static Collection<GrantedAuthority> fromClaims(Map<String, Object> claims, String clientId) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String role : extractClientRoles(claims, clientId)) {
            mapRole(role, authorities);
        }
        return authorities;
    }

    static List<String> extractClientRoles(Map<String, Object> claims, String clientId) {
        Object resourceAccessClaim = claims.get("resource_access");
        if (!(resourceAccessClaim instanceof Map<?, ?> resourceAccess)) {
            return List.of();
        }
        Object clientAccessClaim = resourceAccess.get(clientId);
        if (!(clientAccessClaim instanceof Map<?, ?> clientAccess)) {
            return List.of();
        }
        Object rolesClaim = clientAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return List.of();
        }
        List<String> clientRoles = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof String roleName) {
                clientRoles.add(roleName);
            }
        }
        return clientRoles;
    }

    private static void mapRole(String role, Set<GrantedAuthority> authorities) {
        if (KeycloakClientRoles.WORKSPACE_ADMIN.equals(role)) {
            authorities.add(new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_WORKSPACE_ADMIN));
            authorities.add(new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_EMPLOYEE));
            return;
        }
        if (KeycloakClientRoles.EMPLOYEE.equals(role)) {
            authorities.add(new SimpleGrantedAuthority(KeycloakClientRoles.ROLE_EMPLOYEE));
        }
    }
}
