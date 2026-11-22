package com.vladyslavplus.lokora.resource.dto;

import com.vladyslavplus.lokora.resource.ResourceStatus;
import com.vladyslavplus.lokora.resource.ResourceType;

import java.util.UUID;

public record WorkspaceResourceResponse(
        UUID id,
        String name,
        ResourceType type,
        String location,
        ResourceStatus status,
        Integer capacity
) {
}
