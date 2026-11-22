package com.vladyslavplus.lokora.security;

import java.util.Optional;

public interface CurrentUserAccessor {

    Optional<String> getSubject();

    boolean isWorkspaceAdmin();
}
