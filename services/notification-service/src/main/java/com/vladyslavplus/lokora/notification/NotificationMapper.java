package com.vladyslavplus.lokora.notification;

import com.vladyslavplus.lokora.notification.dto.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}
