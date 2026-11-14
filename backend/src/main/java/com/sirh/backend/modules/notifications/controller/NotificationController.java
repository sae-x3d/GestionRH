package com.sirh.backend.modules.notifications.controller;

import java.util.List;
import com.sirh.backend.modules.auth.entity.AuthUserPrincipal;
import com.sirh.backend.modules.notifications.dto.NotificationResponse;
import com.sirh.backend.modules.notifications.service.NotificationCenterService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationCenterService service;

    public NotificationController(NotificationCenterService service) { this.service = service; }

    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.list(user.getId());
    }

    @GetMapping("/non-lues/count")
    public long unreadCount(@AuthenticationPrincipal AuthUserPrincipal user) {
        return service.unreadCount(user.getId());
    }

    @PostMapping("/{id}/lire")
    public NotificationResponse markRead(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        return service.markRead(user.getId(), id);
    }

    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal AuthUserPrincipal user, @PathVariable Integer id) {
        service.delete(user.getId(), id);
    }
}
