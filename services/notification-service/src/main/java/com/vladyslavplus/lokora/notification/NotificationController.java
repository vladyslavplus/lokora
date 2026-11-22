package com.vladyslavplus.lokora.notification;

import com.vladyslavplus.lokora.notification.dto.NotificationPageResponse;
import com.vladyslavplus.lokora.web.annotation.LokoraRestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@LokoraRestController("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationPageResponse listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size
    ) {
        return notificationService.listMine(page, size);
    }
}
