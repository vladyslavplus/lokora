package com.vladyslavplus.lokora.resource.dto;

import com.vladyslavplus.lokora.resource.ResourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WorkspaceResourceCreateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull ResourceType type,
        @NotBlank @Size(max = 200) String location,
        @NotNull @Min(1) Integer capacity
) {
}
