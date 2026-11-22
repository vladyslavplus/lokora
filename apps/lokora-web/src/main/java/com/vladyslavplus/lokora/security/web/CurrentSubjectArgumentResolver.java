package com.vladyslavplus.lokora.security.web;

import com.vladyslavplus.lokora.security.CurrentUserAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class CurrentSubjectArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserAccessor currentUserAccessor;

    public CurrentSubjectArgumentResolver(CurrentUserAccessor currentUserAccessor) {
        this.currentUserAccessor = currentUserAccessor;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentSubject.class)
                && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        return currentUserAccessor.getSubject()
                .orElseThrow(() -> new IllegalStateException("Authenticated subject is required"));
    }
}
