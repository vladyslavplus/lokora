package com.vladyslavplus.lokora.resource;

import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.reservation.ReservationRepository;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceResourceServiceTest {

    @Mock
    private WorkspaceResourceRepository repository;

    @Mock
    private WorkspaceResourceMapper mapper;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private WorkspaceResourceService service;

    @Test
    void createPersistsNormalizedResourceWithAvailableStatus() {
        WorkspaceResourceCreateRequest request = new WorkspaceResourceCreateRequest(
                "  Desk A  ",
                ResourceType.DESK,
                "  Floor 1  ",
                2
        );
        WorkspaceResource mapped = new WorkspaceResource();
        mapped.setName("  Desk A  ");
        mapped.setType(ResourceType.DESK);
        mapped.setLocation("  Floor 1  ");
        mapped.setCapacity(2);

        when(mapper.toEntity(request)).thenReturn(mapped);
        when(repository.save(any(WorkspaceResource.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(WorkspaceResource.class))).thenAnswer(invocation -> {
            WorkspaceResource saved = invocation.getArgument(0);
            return new WorkspaceResourceResponse(
                    saved.getId(),
                    saved.getName(),
                    saved.getType(),
                    saved.getLocation(),
                    saved.getStatus(),
                    saved.getCapacity()
            );
        });

        WorkspaceResourceResponse response = service.create(request);

        ArgumentCaptor<WorkspaceResource> captor = ArgumentCaptor.forClass(WorkspaceResource.class);
        verify(repository).save(captor.capture());
        WorkspaceResource saved = captor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(saved.getName()).isEqualTo("Desk A");
        assertThat(saved.getLocation()).isEqualTo("Floor 1");
        assertThat(response.status()).isEqualTo(ResourceStatus.AVAILABLE);
        assertThat(response.name()).isEqualTo("Desk A");
    }

    @Test
    void getByIdThrowsWhenResourceMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workspace resource not found");
    }

    @Test
    void updatePreservesIdAndStatusWhileUpdatingEditableFields() {
        UUID id = UUID.randomUUID();
        WorkspaceResource existing = new WorkspaceResource();
        existing.setId(id);
        existing.setName("Old");
        existing.setType(ResourceType.DESK);
        existing.setLocation("Old location");
        existing.setStatus(ResourceStatus.MAINTENANCE);
        existing.setCapacity(1);

        WorkspaceResourceUpdateRequest request = new WorkspaceResourceUpdateRequest(
                "  New name  ",
                ResourceType.MEETING_ROOM,
                "  New location  ",
                4
        );

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(new WorkspaceResourceResponse(
                id,
                "New name",
                ResourceType.MEETING_ROOM,
                "New location",
                ResourceStatus.MAINTENANCE,
                4
        ));
        org.mockito.Mockito.doAnswer(invocation -> {
            WorkspaceResource entity = invocation.getArgument(0);
            WorkspaceResourceUpdateRequest updateRequest = invocation.getArgument(1);
            entity.setName(updateRequest.name());
            entity.setType(updateRequest.type());
            entity.setLocation(updateRequest.location());
            entity.setCapacity(updateRequest.capacity());
            return null;
        }).when(mapper).updateEntity(existing, request);

        WorkspaceResourceResponse response = service.update(id, request);

        verify(mapper).updateEntity(existing, request);
        assertThat(existing.getId()).isEqualTo(id);
        assertThat(existing.getStatus()).isEqualTo(ResourceStatus.MAINTENANCE);
        assertThat(existing.getName()).isEqualTo("New name");
        assertThat(existing.getLocation()).isEqualTo("New location");
        assertThat(response.status()).isEqualTo(ResourceStatus.MAINTENANCE);
        verify(repository).save(existing);
    }

    @Test
    void changeStatusUpdatesOnlyStatus() {
        UUID id = UUID.randomUUID();
        WorkspaceResource existing = new WorkspaceResource();
        existing.setId(id);
        existing.setName("Desk");
        existing.setType(ResourceType.DESK);
        existing.setLocation("Floor");
        existing.setStatus(ResourceStatus.AVAILABLE);
        existing.setCapacity(1);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(new WorkspaceResourceResponse(
                id,
                "Desk",
                ResourceType.DESK,
                "Floor",
                ResourceStatus.OUT_OF_SERVICE,
                1
        ));

        WorkspaceResourceResponse response = service.changeStatus(
                id,
                new WorkspaceResourceStatusChangeRequest(ResourceStatus.OUT_OF_SERVICE)
        );

        assertThat(existing.getStatus()).isEqualTo(ResourceStatus.OUT_OF_SERVICE);
        assertThat(existing.getName()).isEqualTo("Desk");
        assertThat(response.status()).isEqualTo(ResourceStatus.OUT_OF_SERVICE);
        verify(repository).save(existing);
        verify(mapper, never()).updateEntity(any(), any());
    }

    @Test
    void deleteRemovesResourceWhenNoReservationHistory() {
        UUID id = UUID.randomUUID();
        WorkspaceResource existing = new WorkspaceResource();
        existing.setId(id);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existing));
        when(reservationRepository.existsByResourceId(id)).thenReturn(false);

        service.delete(id);

        verify(repository).findByIdForUpdate(id);
        verify(repository, never()).findById(id);
        verify(repository).delete(existing);
    }

    @Test
    void deleteThrowsWhenResourceHasReservationHistory() {
        UUID id = UUID.randomUUID();
        WorkspaceResource existing = new WorkspaceResource();
        existing.setId(id);
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.of(existing));
        when(reservationRepository.existsByResourceId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(com.vladyslavplus.lokora.common.exception.ResourceHasReservationsException.class);

        verify(repository).findByIdForUpdate(id);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenResourceMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).delete(any());
    }

    @Test
    void listUsesDefaultPageSizeAndDeterministicSort() {
        when(repository.findAllByOrderByNameAscIdAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        WorkspaceResourcePageResponse response = service.list(0, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByNameAscIdAsc(pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("name").ascending().and(Sort.by("id").ascending()));
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void listAppliesRequestedPageAndSize() {
        when(repository.findAllByOrderByNameAscIdAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 25));

        WorkspaceResourcePageResponse response = service.list(2, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByNameAscIdAsc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(25);
    }

    @Test
    void listCapsPageSizeAtMaximum() {
        when(repository.findAllByOrderByNameAscIdAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        service.list(0, 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAllByOrderByNameAscIdAsc(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }
}
