package com.vladyslavplus.lokora.security.web;

import com.vladyslavplus.lokora.security.SpringSecurityCurrentUserAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CurrentSubjectControllerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsResolvedSubjectInHttpResponse() throws Exception {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("jwt-subject")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, "n/a", "ROLE_EMPLOYEE")
        );

        CurrentSubjectArgumentResolver resolver = new CurrentSubjectArgumentResolver(
                new SpringSecurityCurrentUserAccessor()
        );
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new CurrentSubjectController())
                .setCustomArgumentResolvers(resolver)
                .build();

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subject").value("jwt-subject"));
    }
}
