package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.common.exception.ReservationForbiddenException;
import com.vladyslavplus.lokora.common.exception.ReservationNotFoundException;
import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.common.exception.ReservationStateConflictException;
import com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException;
import com.vladyslavplus.lokora.common.http.DownstreamRestClientFactory;
import com.vladyslavplus.lokora.reservation.dto.ReservationConfirmationResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.security.DownstreamAccessTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.core.task.TaskRejectedException;

import java.util.UUID;
import java.time.Duration;

@Component
public class HttpReservationApplicationClient implements ReservationApplicationClient {
    private final RestClient restClient;
    private final DownstreamAccessTokenProvider tokenProvider;

    public HttpReservationApplicationClient(
            RestClient.Builder builder,
            DownstreamAccessTokenProvider tokenProvider,
            @Value("${lokora.services.reservation.base-url}") String baseUrl,
            @Value("${lokora.services.connect-timeout:2s}") Duration connectTimeout,
            @Value("${lokora.services.read-timeout:5s}") Duration readTimeout
    ) {
        this.restClient = DownstreamRestClientFactory.create(builder, baseUrl, connectTimeout, readTimeout);
        this.tokenProvider = tokenProvider;
    }

    @Override
    public ReservationPageResponse listMine(int page, Integer size) {
        try {
            return restClient.get().uri(uri -> uri.path("/api/reservations/mine").queryParam("page", page)
                            .queryParamIfPresent("size", java.util.Optional.ofNullable(size)).build())
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(ReservationPageResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public ReservationResponse create(ReservationCreateRequest request) {
        try {
            return restClient.post().uri("/api/reservations").headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .body(request).retrieve().body(ReservationResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public ReservationResponse cancel(UUID id) {
        try {
            return restClient.post().uri("/api/reservations/{id}/cancel", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(ReservationResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public ReservationConfirmationResponse prepareConfirmation(UUID id) {
        try {
            return restClient.post().uri("/api/reservations/{id}/confirmation", id)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(ReservationConfirmationResponse.class);
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    @Override
    public boolean hasResourceReservations(UUID resourceId) {
        try {
            ReservationHistoryResponse response = restClient.get()
                    .uri("/api/reservations/internal/resources/{id}/has-history", resourceId)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.currentAccessToken()))
                    .retrieve().body(ReservationHistoryResponse.class);
            return response != null && response.hasHistory();
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        }
    }

    private RuntimeException translate(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == 404) {
            return new ReservationNotFoundException();
        }
        if (status.value() == 403) {
            return new ReservationForbiddenException();
        }
        if (status.value() == 409) {
            return switch (downstreamErrorCode(exception)) {
                case "RESOURCE_UNAVAILABLE" -> new ResourceUnavailableForReservationException();
                case "RESERVATION_STATE_CONFLICT" -> new ReservationStateConflictException("Reservation state does not allow this operation");
                default -> new ReservationOverlapException();
            };
        }
        if (status.value() == 503 && "ASYNC_CAPACITY_EXCEEDED".equals(downstreamErrorCode(exception))) {
            return new TaskRejectedException("Reservation service async capacity is full");
        }
        return exception;
    }

    private String downstreamErrorCode(RestClientResponseException exception) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\\"code\\\"\\s*:\\s*\\\"([A-Z_]+)\\\"")
                .matcher(exception.getResponseBodyAsString());
        return matcher.find() ? matcher.group(1) : "";
    }

    private record ReservationHistoryResponse(boolean hasHistory) {
    }
}
