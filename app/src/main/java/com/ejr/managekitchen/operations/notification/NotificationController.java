package com.ejr.managekitchen.operations.notification;

import com.ejr.managekitchen.common.security.AuthenticatedUser;
import com.ejr.managekitchen.operations.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "F7 — historique des notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Mes notifications, les plus récentes d'abord")
    public List<NotificationResponse> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return notificationService.findMine(user);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public NotificationResponse markRead(@PathVariable("id") Long id,
                                         @AuthenticationPrincipal AuthenticatedUser user) {
        return notificationService.markRead(id, user);
    }
}
