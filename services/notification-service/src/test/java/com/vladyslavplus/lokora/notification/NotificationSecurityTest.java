package com.vladyslavplus.lokora.notification;

import com.vladyslavplus.lokora.contracts.reservation.ReservationEventType;
import com.vladyslavplus.lokora.notification.dto.NotificationPageResponse;
import com.vladyslavplus.lokora.notification.dto.NotificationResponse;
import com.vladyslavplus.lokora.support.ApiSecurityTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "lokora.security.keycloak.client-id=lokora-app",
        "lokora.security.keycloak.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:9/realms/lokora"
})
class NotificationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void listNotifications_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listNotifications_authenticatedWithoutApplicationRole_returns403() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .with(jwt().jwt(token -> token.subject("someone"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listNotifications_employee_returnsOwnNotifications() throws Exception {
        UUID id = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(notificationService.listMine(0, null)).thenReturn(new NotificationPageResponse(
                List.of(new NotificationResponse(
                        id,
                        ReservationEventType.CREATED,
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "Reservation confirmed",
                        Instant.parse("2026-10-01T00:00:00Z")
                )),
                0, 20, 1, 1
        ));

        mockMvc.perform(get("/api/notifications")
                        .with(jwt().jwt(token -> token.subject("employee-1"))
                                .authorities(() -> "ROLE_EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].eventType").value("CREATED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listNotifications_invalidPageParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/notifications").param("page", "abc")
                        .with(jwt().jwt(token -> token.subject("employee-1"))
                                .authorities(() -> "ROLE_EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }
}
