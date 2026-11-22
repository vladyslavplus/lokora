package com.vladyslavplus.lokora.resource.dto;

import com.vladyslavplus.lokora.resource.ResourceStatus;
import jakarta.validation.constraints.NotNull;

public record WorkspaceResourceStatusChangeRequest(
        @NotNull ResourceStatus status
) {
}
