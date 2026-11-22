package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;

import java.util.UUID;

public interface ResourceApplicationClient {
    WorkspaceResourcePageResponse list(int page, Integer size);
    WorkspaceResourceResponse getById(UUID id);
    WorkspaceResourceResponse create(WorkspaceResourceCreateRequest request);
    WorkspaceResourceResponse update(UUID id, WorkspaceResourceUpdateRequest request);
    WorkspaceResourceResponse changeStatus(UUID id, WorkspaceResourceStatusChangeRequest request);
    void delete(UUID id);
}
