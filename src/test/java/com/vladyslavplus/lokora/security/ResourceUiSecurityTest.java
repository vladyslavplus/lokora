package com.vladyslavplus.lokora.security;

import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.support.UiSecurityTestApplication;
import com.vladyslavplus.lokora.support.OAuth2TestClientConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UiSecurityTestApplication.class)
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
class ResourceUiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceResourceService resourceService;

    @Test
    void protectedUiPageRedirectsUnauthenticatedUserToOidcLogin() throws Exception {
        mockMvc.perform(get("/resources"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oauth2/authorization/keycloak"));
    }

    @Test
    void authenticatedEmployeeCanViewResources() throws Exception {
        when(resourceService.list(anyInt(), any())).thenReturn(emptyPage());

        mockMvc.perform(get("/resources")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE)))
                .andExpect(status().isOk());
    }

    @Test
    void employeeCannotSubmitAdministratorUiMutationThroughCraftedRequest() throws Exception {
        mockMvc.perform(post("/resources")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE))
                        .with(csrf())
                        .param("name", "Desk")
                        .param("type", "DESK")
                        .param("location", "Floor 1")
                        .param("capacity", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorUiMutationSucceedsAtSecurityLayer() throws Exception {
        UUID id = UUID.randomUUID();
        when(resourceService.create(any(WorkspaceResourceCreateRequest.class))).thenReturn(sampleResource(id));

        mockMvc.perform(post("/resources")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.WORKSPACE_ADMIN))
                        .with(csrf())
                        .param("name", "Desk")
                        .param("type", "DESK")
                        .param("location", "Floor 1")
                        .param("capacity", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/resources/" + id));
    }

    @Test
    void uiMutationRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/resources")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.WORKSPACE_ADMIN))
                        .param("name", "Desk")
                        .param("type", "DESK")
                        .param("location", "Floor 1")
                        .param("capacity", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiChainDoesNotAuthenticateThroughUiSession() throws Exception {
        when(resourceService.list(anyInt(), any())).thenReturn(emptyPage());

        MockHttpSession session = (MockHttpSession) mockMvc.perform(get("/resources")
                        .with(OidcLoginTestSupport.oidcLoginWithClientRole(KeycloakClientRoles.EMPLOYEE)))
                .andExpect(status().isOk())
                .andReturn()
                .getRequest()
                .getSession();

        mockMvc.perform(get("/api/resources").session(session))
                .andExpect(status().isUnauthorized());
    }

    private WorkspaceResourcePageResponse emptyPage() {
        return new WorkspaceResourcePageResponse(List.of(), 0, 20, 0, 0);
    }

    private WorkspaceResourceResponse sampleResource(UUID id) {
        return new WorkspaceResourceResponse(
                id,
                "Desk",
                com.vladyslavplus.lokora.resource.ResourceType.DESK,
                "Floor 1",
                com.vladyslavplus.lokora.resource.ResourceStatus.AVAILABLE,
                1
        );
    }
}
