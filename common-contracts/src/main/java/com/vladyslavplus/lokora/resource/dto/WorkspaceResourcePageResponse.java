package com.vladyslavplus.lokora.resource.dto;

import java.util.List;

public record WorkspaceResourcePageResponse(
        List<WorkspaceResourceResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
