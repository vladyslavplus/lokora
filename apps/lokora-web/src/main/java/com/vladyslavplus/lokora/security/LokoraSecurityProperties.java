package com.vladyslavplus.lokora.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lokora.security.keycloak")
public record LokoraSecurityProperties(
        String clientId,
        String issuerUri
) {
}
