package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.ReservationStatus;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import com.vladyslavplus.lokora.reservation.dto.ReservationResponse;
import com.vladyslavplus.lokora.resource.ResourceStatus;
import com.vladyslavplus.lokora.resource.ResourceType;
import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.security.KeycloakClientRoles;
import com.vladyslavplus.lokora.security.OidcLoginTestSupport;
import com.vladyslavplus.lokora.support.OAuth2TestClientConfiguration;
import com.vladyslavplus.lokora.support.ReservationUiTestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(classes = ReservationUiTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "lokora.security.keycloak.client-id=lokora-app",
        "lokora.security.keycloak.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.client.registration.keycloak.client-id=lokora-app",
        "spring.security.oauth2.client.registration.keycloak.client-secret=test-secret",
        "LOKORA_OIDC_CLIENT_SECRET=test-secret",
        "spring.security.oauth2.client.registration.keycloak.authorization-grant-type=authorization_code",
        "spring.security.oauth2.client.registration.keycloak.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
        "spring.security.oauth2.client.registration.keycloak.scope=openid,profile,email",
        "spring.security.oauth2.client.provider.keycloak.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:9/realms/lokora"
})
@Import(OAuth2TestClientConfiguration.class)
class ReservationUiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @MockitoBean
    private WorkspaceResourceService resourceService;

    @Test
    void minePageIsMappedUnderReservationsPrefix() throws Exception {
        when(reservationService.listMine(0, null)).thenReturn(emptyPage());

        mockMvc.perform(get("/reservations/mine")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/mine"));
    }

    @Test
    void reserveFormIsMappedUnderResourcesPrefix() throws Exception {
        UUID resourceId = UUID.randomUUID();
        when(resourceService.getById(resourceId)).thenReturn(sampleResource(resourceId));

        mockMvc.perform(get("/resources/{id}/reserve", resourceId)
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(view().name("reservations/form"));
    }

    @Test
    void reserveSubmitRedirectsToMinePage() throws Exception {
        UUID resourceId = UUID.randomUUID();
        when(resourceService.getById(resourceId)).thenReturn(sampleResource(resourceId));
        when(reservationService.create(any())).thenReturn(sampleReservation(resourceId));

        mockMvc.perform(post("/resources/{id}/reserve", resourceId)
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE))
                        .with(csrf())
                        .param("startTime", "2026-09-22T10:00:00+00:00")
                        .param("endTime", "2026-09-22T11:00:00+00:00"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void cancelIsMappedUnderReservationsPrefix() throws Exception {
        UUID reservationId = UUID.randomUUID();
        when(reservationService.cancel(reservationId)).thenReturn(
                new ReservationResponse(
                        reservationId,
                        UUID.randomUUID(),
                        "user-1",
                        Instant.parse("2026-09-22T10:00:00Z"),
                        Instant.parse("2026-09-22T11:00:00Z"),
                        ReservationStatus.CANCELLED
                )
        );

        mockMvc.perform(post("/reservations/{id}/cancel", reservationId)
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        verify(reservationService).cancel(reservationId);
    }

    @Test
    void reserveSubmitRequiresCsrfToken() throws Exception {
        UUID resourceId = UUID.randomUUID();
        when(resourceService.getById(resourceId)).thenReturn(sampleResource(resourceId));

        mockMvc.perform(post("/resources/{id}/reserve", resourceId)
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE))
                        .param("startTime", "2026-09-22T10:00:00+00:00")
                        .param("endTime", "2026-09-22T11:00:00+00:00"))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/reservations/{id}/cancel", UUID.randomUUID())
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE)))
                .andExpect(status().isForbidden());
    }

    private ReservationPageResponse emptyPage() {
        return new ReservationPageResponse(List.of(), 0, 20, 0, 0);
    }

    private WorkspaceResourceResponse sampleResource(UUID id) {
        return new WorkspaceResourceResponse(
                id,
                "Desk",
                ResourceType.DESK,
                "Floor 1",
                ResourceStatus.AVAILABLE,
                1
        );
    }

    private ReservationResponse sampleReservation(UUID resourceId) {
        return new ReservationResponse(
                UUID.randomUUID(),
                resourceId,
                "user-1",
                Instant.parse("2026-09-22T10:00:00Z"),
                Instant.parse("2026-09-22T11:00:00Z"),
                ReservationStatus.ACTIVE
        );
    }
}
