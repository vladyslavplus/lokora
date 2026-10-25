package com.vladyslavplus.lokora.reservation;

import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.support.ApiSecurityTestApplication;
import com.vladyslavplus.lokora.support.OAuth2TestClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "lokora.security.keycloak.client-id=lokora-app",
        "lokora.security.keycloak.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.client.registration.keycloak.client-secret=test-secret",
        "LOKORA_OIDC_CLIENT_SECRET=test-secret"
})
@Import({ReservationController.class, OAuth2TestClientConfiguration.class})
class ReservationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private WorkspaceResourceService workspaceResourceService;

    @Test
    void unauthenticatedReservationApiRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/reservations/mine"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedEmployeeCanListOwnReservations() throws Exception {
        when(reservationService.listMine(0, null)).thenReturn(
                new ReservationPageResponse(List.of(), 0, 20, 0, 0)
        );

        mockMvc.perform(get("/api/reservations/mine")
                        .with(jwt().jwt(token -> token.subject("employee-1"))
                                .authorities(() -> "ROLE_EMPLOYEE")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedEmployeeCanCreateReservation() throws Exception {
        when(reservationService.create(any())).thenReturn(
                new ReservationResponse(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "employee-1",
                        java.time.Instant.parse("2026-09-22T10:00:00Z"),
                        java.time.Instant.parse("2026-09-22T11:00:00Z"),
                        ReservationStatus.ACTIVE
                )
        );

        mockMvc.perform(post("/api/reservations")
                        .with(jwt().jwt(token -> token.subject("employee-1"))
                                .authorities(() -> "ROLE_EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resourceId": "11111111-1111-1111-1111-111111111111",
                                  "startTime": "2026-09-22T10:00:00Z",
                                  "endTime": "2026-09-22T11:00:00Z"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
