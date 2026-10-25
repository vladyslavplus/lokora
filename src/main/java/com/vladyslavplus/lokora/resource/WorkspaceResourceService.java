package com.vladyslavplus.lokora.resource;

import com.vladyslavplus.lokora.common.exception.ResourceHasReservationsException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.reservation.ReservationRepository;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkspaceResourceService {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceResourceRepository repository;
    private final WorkspaceResourceMapper mapper;
    private final ReservationRepository reservationRepository;

    public WorkspaceResourceService(
            WorkspaceResourceRepository repository,
            WorkspaceResourceMapper mapper,
            ReservationRepository reservationRepository
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public WorkspaceResourceResponse create(WorkspaceResourceCreateRequest request) {
        WorkspaceResource entity = mapper.toEntity(request);
        entity.setId(UUID.randomUUID());
        entity.setStatus(ResourceStatus.AVAILABLE);
        normalizeEditableFields(entity);
        WorkspaceResource saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    public WorkspaceResourcePageResponse list(int page, Integer size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = size == null || size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(
                resolvedPage,
                resolvedSize,
                Sort.by("name").ascending().and(Sort.by("id").ascending())
        );
        Page<WorkspaceResource> pageResult = repository.findAllByOrderByNameAscIdAsc(pageable);
        return new WorkspaceResourcePageResponse(
                pageResult.getContent().stream().map(mapper::toResponse).toList(),
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'WORKSPACE_ADMIN')")
    public WorkspaceResourceResponse getById(UUID id) {
        WorkspaceResource entity = findExisting(id);
        return mapper.toResponse(entity);
    }

    @Transactional
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public WorkspaceResourceResponse update(UUID id, WorkspaceResourceUpdateRequest request) {
        WorkspaceResource entity = findExisting(id);
        ResourceStatus previousStatus = entity.getStatus();
        mapper.updateEntity(entity, request);
        entity.setStatus(previousStatus);
        normalizeEditableFields(entity);
        WorkspaceResource saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public WorkspaceResourceResponse changeStatus(UUID id, WorkspaceResourceStatusChangeRequest request) {
        WorkspaceResource entity = findExisting(id);
        entity.setStatus(request.status());
        WorkspaceResource saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    @Transactional
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public void delete(UUID id) {
        WorkspaceResource entity = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace resource not found"));
        if (reservationRepository.existsByResourceId(id)) {
            throw new ResourceHasReservationsException();
        }
        repository.delete(entity);
    }

    private WorkspaceResource findExisting(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace resource not found"));
    }

    private void normalizeEditableFields(WorkspaceResource entity) {
        entity.setName(entity.getName().trim());
        entity.setLocation(entity.getLocation().trim());
    }
}
