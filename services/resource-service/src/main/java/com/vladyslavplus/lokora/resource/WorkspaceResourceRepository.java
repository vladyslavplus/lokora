package com.vladyslavplus.lokora.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

public interface WorkspaceResourceRepository extends JpaRepository<WorkspaceResource, UUID> {

    Page<WorkspaceResource> findAllByOrderByNameAscIdAsc(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM WorkspaceResource r WHERE r.id = :id")
    Optional<WorkspaceResource> findByIdForUpdate(@Param("id") UUID id);
}
