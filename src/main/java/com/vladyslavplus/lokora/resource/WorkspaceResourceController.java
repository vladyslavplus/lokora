package com.vladyslavplus.lokora.resource;

import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import com.vladyslavplus.lokora.web.annotation.LokoraRestController;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@LokoraRestController("/api/resources")
public class WorkspaceResourceController {

    private final WorkspaceResourceService service;

    public WorkspaceResourceController(WorkspaceResourceService service) {
        this.service = service;
    }

    @GetMapping
    public WorkspaceResourcePageResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return service.list(page, size);
    }

    @GetMapping("/{id}")
    public WorkspaceResourceResponse get(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResourceResponse> create(
            @Valid @RequestBody WorkspaceResourceCreateRequest request
    ) {
        WorkspaceResourceResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public WorkspaceResourceResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody WorkspaceResourceUpdateRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public WorkspaceResourceResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody WorkspaceResourceStatusChangeRequest request
    ) {
        return service.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
