package com.vladyslavplus.lokora.security.web;

import com.vladyslavplus.lokora.security.SpringSecurityCurrentUserAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentSubjectArgumentResolverTest {

    private final CurrentSubjectArgumentResolver resolver =
            new CurrentSubjectArgumentResolver(new SpringSecurityCurrentUserAccessor());

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supportsOnlyAnnotatedStringParameters() throws Exception {
        assertThat(resolver.supportsParameter(parameter("annotatedString"))).isTrue();
        assertThat(resolver.supportsParameter(parameter("plainString"))).isFalse();
        assertThat(resolver.supportsParameter(parameter("annotatedUuid"))).isFalse();
    }

    @Test
    void resolvesSubjectFromJwtPrincipal() throws Exception {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("jwt-subject")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, "n/a", "ROLE_EMPLOYEE")
        );

        Object value = resolver.resolveArgument(
                parameter("annotatedString"), null, new ServletWebRequest(new MockHttpServletRequest()), null
        );

        assertThat(value).isInstanceOf(String.class).isEqualTo("jwt-subject");
    }

    @Test
    void resolvesSubjectFromOidcPrincipal() throws Exception {
        OidcUser oidcUser = mock(OidcUser.class);
        when(oidcUser.getSubject()).thenReturn("oidc-subject");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(oidcUser, "n/a", List.of())
        );

        Object value = resolver.resolveArgument(
                parameter("annotatedString"), null, new ServletWebRequest(new MockHttpServletRequest()), null
        );

        assertThat(value).isInstanceOf(String.class).isEqualTo("oidc-subject");
    }

    @Test
    void rejectsMissingAuthenticatedSubject() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(
                parameter("annotatedString"), null, new ServletWebRequest(new MockHttpServletRequest()), null
        )).isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated subject is required");
    }

    private MethodParameter parameter(String methodName) throws NoSuchMethodException {
        return new MethodParameter(ResolverParameters.class.getDeclaredMethod(methodName, parameterType(methodName)), 0);
    }

    private Class<?> parameterType(String methodName) {
        return methodName.equals("annotatedUuid") ? UUID.class : String.class;
    }

    private static class ResolverParameters {
        void annotatedString(@CurrentSubject String subject) {
        }

        void plainString(String value) {
        }

        void annotatedUuid(@CurrentSubject UUID id) {
        }
    }
}
