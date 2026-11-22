package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.contracts.resource.ResourceReservationAvailabilityResponse;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException;
import com.vladyslavplus.lokora.common.exception.DownstreamServiceUnavailableException;
import com.vladyslavplus.lokora.common.exception.ReservationForbiddenException;
import com.vladyslavplus.lokora.common.http.DownstreamRestClientFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;
import java.time.Duration;

@Component
public class HttpResourceAvailabilityClient implements ResourceAvailabilityClient {
    private final RestClient restClient;

    public HttpResourceAvailabilityClient(
            RestClient.Builder builder,
            @Value("${lokora.services.resource.base-url}") String resourceBaseUrl,
            @Value("${lokora.services.connect-timeout:2s}") Duration connectTimeout,
            @Value("${lokora.services.read-timeout:5s}") Duration readTimeout
    ) {
        this.restClient = DownstreamRestClientFactory.create(builder, resourceBaseUrl, connectTimeout, readTimeout);
    }

    @Override
    public boolean isAvailable(UUID resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)) {
            throw new IllegalStateException("A user JWT is required to validate a resource");
        }

        try {
            ResourceReservationAvailabilityResponse response = restClient.get()
                    .uri("/api/resources/internal/resources/{id}/reservation-availability", resourceId)
                    .headers(headers -> headers.setBearerAuth(jwtAuthentication.getToken().getTokenValue()))
                    .retrieve()
                    .body(ResourceReservationAvailabilityResponse.class);
            if (response == null) {
                throw new IllegalStateException("Resource service returned an empty response");
            }
            return response.available();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Workspace resource not found");
            }
            if (exception.getStatusCode().value() == 403) {
                throw new ReservationForbiddenException();
            }
            if (exception.getStatusCode().is5xxServerError()) {
                throw new DownstreamServiceUnavailableException("Resource service");
            }
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new DownstreamServiceUnavailableException("Resource service");
        }
    }
}
