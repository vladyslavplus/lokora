package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.common.exception.ResourceHasReservationsException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.common.http.DownstreamRestClientFactory;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import com.vladyslavplus.lokora.security.DownstreamAccessTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.UUID;
import java.time.Duration;

@Component
public class HttpResourceApplicationClient implements ResourceApplicationClient {
    private final RestClient restClient;
    private final DownstreamAccessTokenProvider tokenProvider;
    private final ReservationApplicationClient reservationClient;

    public HttpResourceApplicationClient(
            RestClient.Builder builder,
            DownstreamAccessTokenProvider tokenProvider,
            ReservationApplicationClient reservationClient,
            @Value("${lokora.services.resource.base-url}") String baseUrl,
            @Value("${lokora.services.connect-timeout:2s}") Duration connectTimeout,
            @Value("${lokora.services.read-timeout:5s}") Duration readTimeout
    ) {
        this.restClient = DownstreamRestClientFactory.create(builder, baseUrl, connectTimeout, readTimeout);
        this.tokenProvider = tokenProvider;
        this.reservationClient = reservationClient;
    }

    @Override
    public WorkspaceResourcePageResponse list(int page, Integer size) {
        try {
            return restClient.get().uri(uri -> uri.path("/api/resources").queryParam("page", page)
                            .queryParamIfPresent("size", java.util.Optional.ofNullable(size)).build())
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(WorkspaceResourcePageResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public WorkspaceResourceResponse getById(UUID id) {
        try {
            return restClient.get().uri("/api/resources/{id}", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(WorkspaceResourceResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public WorkspaceResourceResponse create(WorkspaceResourceCreateRequest request) {
        try {
            return restClient.post().uri("/api/resources").headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .body(request).retrieve().body(WorkspaceResourceResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public WorkspaceResourceResponse update(UUID id, WorkspaceResourceUpdateRequest request) {
        try {
            return restClient.put().uri("/api/resources/{id}", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .body(request).retrieve().body(WorkspaceResourceResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public WorkspaceResourceResponse changeStatus(UUID id, WorkspaceResourceStatusChangeRequest request) {
        try {
            return restClient.patch().uri("/api/resources/{id}/status", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .body(request).retrieve().body(WorkspaceResourceResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public void delete(UUID id) {
        if (reservationClient.hasResourceReservations(id)) {
            throw new ResourceHasReservationsException();
        }
        try {
            restClient.delete().uri("/api/resources/internal/resources/{id}", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    private RuntimeException translate(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == 404) {
            return new ResourceNotFoundException("Workspace resource not found");
        }
        if (status.value() == 409) {
            return new ResourceHasReservationsException();
        }
        return exception;
    }
}
