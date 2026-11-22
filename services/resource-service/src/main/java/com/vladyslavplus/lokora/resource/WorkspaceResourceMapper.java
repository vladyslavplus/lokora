package com.vladyslavplus.lokora.resource;

import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface WorkspaceResourceMapper {

    WorkspaceResourceResponse toResponse(WorkspaceResource entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    WorkspaceResource toEntity(WorkspaceResourceCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    void updateEntity(@MappingTarget WorkspaceResource entity, WorkspaceResourceUpdateRequest request);
}
