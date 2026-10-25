package com.vladyslavplus.lokora.security;

import com.vladyslavplus.lokora.reservation.ReservationRepository;
import com.vladyslavplus.lokora.resource.WorkspaceResourceMapper;
import com.vladyslavplus.lokora.resource.WorkspaceResourceRepository;
import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
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
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ApiSecurityTestApplication.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "lokora.security.keycloak.client-id=lokora-app",
        "lokora.security.keycloak.issuer-uri=http://127.0.0.1:9/realms/lokora",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://127.0.0.1:9/realms/lokora"
})
@Import({WorkspaceResourceService.class, OAuth2TestClientConfiguration.class})
class WorkspaceResourceSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceResourceRepository repository;

    @MockitoBean
    private WorkspaceResourceMapper mapper;

    @MockitoBean
    private ReservationRepository reservationRepository;

    @Test
    void apiRequestWithoutJwtReturns401WithoutRedirect() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void employeeJwtCanReadResources() throws Exception {
        when(repository.findAllByOrderByNameAscIdAsc(any())).thenReturn(
                new org.springframework.data.domain.PageImpl<>(List.of())
        );

        mockMvc.perform(get("/api/resources")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void workspaceAdminJwtCanCreateResource() throws Exception {
        UUID id = UUID.randomUUID();
        java.util.Map<String, Object> state = new java.util.HashMap<>();
        com.vladyslavplus.lokora.resource.WorkspaceResource entity =
                org.mockito.Mockito.mock(com.vladyslavplus.lokora.resource.WorkspaceResource.class);
        org.mockito.Mockito.when(entity.getName()).thenAnswer(invocation -> state.get("name"));
        org.mockito.Mockito.when(entity.getLocation()).thenAnswer(invocation -> state.get("location"));
        org.mockito.Mockito.doAnswer(invocation -> {
            state.put("name", invocation.getArgument(0));
            return null;
        }).when(entity).setName(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doAnswer(invocation -> {
            state.put("location", invocation.getArgument(0));
            return null;
        }).when(entity).setLocation(org.mockito.ArgumentMatchers.any());
        when(mapper.toEntity(any())).thenAnswer(invocation -> {
            com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest request = invocation.getArgument(0);
            state.put("name", request.name());
            state.put("location", request.location());
            return entity;
        });
        when(repository.save(any())).thenAnswer(invocation -> {
            com.vladyslavplus.lokora.resource.WorkspaceResource saved = invocation.getArgument(0);
            saved.setId(id);
            saved.setStatus(com.vladyslavplus.lokora.resource.ResourceStatus.AVAILABLE);
            return saved;
        });
        when(mapper.toResponse(any())).thenReturn(sampleResource(id));

        mockMvc.perform(post("/api/resources")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_WORKSPACE_ADMIN")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Desk",
                                  "type": "DESK",
                                  "location": "Floor 1",
                                  "capacity": 1
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void employeeJwtReceivesFilterChain403ForAdministratorCreate() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Desk",
                                  "type": "DESK",
                                  "location": "Floor 1",
                                  "capacity": 1
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void employeeJwtReceivesMethodSecurity403ForStatusChange() throws Exception {
        UUID id = UUID.randomUUID();
        com.vladyslavplus.lokora.resource.WorkspaceResource entity =
                org.mockito.Mockito.mock(com.vladyslavplus.lokora.resource.WorkspaceResource.class);
        entity.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        mockMvc.perform(patch("/api/resources/{id}/status", id)
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "MAINTENANCE"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void authenticatedTokenWithoutLokoraRolesReceives403OnRead() throws Exception {
        mockMvc.perform(get("/api/resources")
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_openid")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void employeeJwtReceivesFilterChain403ForUpdateAndDelete() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/resources/{id}", id)
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Desk",
                                  "type": "DESK",
                                  "location": "Floor 1",
                                  "capacity": 1
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/resources/{id}", id)
                        .with(jwt().authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_EMPLOYEE")
                        )))
                .andExpect(status().isForbidden());
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
