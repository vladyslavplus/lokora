package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/resources")
public class ResourceBffController {
    private final ResourceApplicationClient resourceClient;

    public ResourceBffController(ResourceApplicationClient resourceClient) {
        this.resourceClient = resourceClient;
    }

    @GetMapping
    public WorkspaceResourcePageResponse list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(required = false) Integer size) {
        return resourceClient.list(page, size);
    }

    @GetMapping("/{id}")
    public WorkspaceResourceResponse get(@PathVariable UUID id) {
        return resourceClient.getById(id);
    }

    @PostMapping
    public ResponseEntity<WorkspaceResourceResponse> create(@Valid @RequestBody WorkspaceResourceCreateRequest request) {
        WorkspaceResourceResponse created = resourceClient.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public WorkspaceResourceResponse update(@PathVariable UUID id,
                                            @Valid @RequestBody WorkspaceResourceUpdateRequest request) {
        return resourceClient.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public WorkspaceResourceResponse changeStatus(@PathVariable UUID id,
                                                  @Valid @RequestBody WorkspaceResourceStatusChangeRequest request) {
        return resourceClient.changeStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        resourceClient.delete(id);
        return ResponseEntity.noContent().build();
    }
}
