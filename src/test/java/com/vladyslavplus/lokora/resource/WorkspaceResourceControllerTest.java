package com.vladyslavplus.lokora.resource;

import com.vladyslavplus.lokora.common.api.GlobalExceptionHandler;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class WorkspaceResourceControllerTest {

    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private WorkspaceResourceService service;

    private MockMvc mockMvc;

    @Test
    void composedControllerReturnsResourcePageAtExistingRoute() throws Exception {
        WorkspaceResourceResponse resource = new WorkspaceResourceResponse(
                RESOURCE_ID, "Desk A", ResourceType.DESK, "Floor 1", ResourceStatus.AVAILABLE, 1
        );
        when(service.list(0, null)).thenReturn(new WorkspaceResourcePageResponse(List.of(resource), 0, 20, 1, 1));

        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(RESOURCE_ID.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Desk A"));
    }

    @Test
    void missingResourceAtComposedRouteReturns404() throws Exception {
        when(service.getById(RESOURCE_ID)).thenThrow(new ResourceNotFoundException("Workspace resource not found"));

        mockMvc.perform(get("/api/resources/{id}", RESOURCE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @BeforeEach
    void setUp() {
        GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler(Clock.systemUTC());
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkspaceResourceController(service))
                .setControllerAdvice(exceptionHandler)
                .build();
    }

    @Test
    void invalidResourceIdReturnsStableApiErrorResponse() throws Exception {
        mockMvc.perform(get("/api/resources/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"))
                .andExpect(jsonPath("$.path").value("/api/resources/not-a-uuid"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void malformedJsonReturnsStableApiErrorResponse() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{name:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or unreadable"))
                .andExpect(jsonPath("$.path").value("/api/resources"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void invalidEnumJsonReturnsStableApiErrorResponse() throws Exception {
        mockMvc.perform(post("/api/resources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Desk A",
                                  "type": "INVALID",
                                  "location": "Floor 1",
                                  "capacity": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Request body is malformed or unreadable"))
                .andExpect(jsonPath("$.path").value("/api/resources"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
