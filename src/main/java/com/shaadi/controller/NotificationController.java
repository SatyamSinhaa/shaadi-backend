package com.shaadi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.Notification;
import com.shaadi.service.NotificationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin
public class NotificationController {
    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getNotifications(@PathVariable int userId) {
        try {
            List<Notification> notifications = notificationService.getNotificationsForUser(userId);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving notifications"));
        }
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<?> getUnreadNotifications(@PathVariable int userId) {
        try {
            List<Notification> notifications = notificationService.getUnreadNotificationsForUser(userId);
            return ResponseEntity.ok(notifications);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving unread notifications"));
        }
    }

    @GetMapping("/{userId}/unread/count")
    public ResponseEntity<?> getUnreadNotificationCount(@PathVariable int userId) {
        try {
            long count = notificationService.getUnreadNotificationCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving notification count"));
        }
    }

    @PostMapping("/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable long notificationId, @RequestBody Map<String, Integer> requestBody) {
        try {
            Integer userId = requestBody.get("userId");
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }
            notificationService.markNotificationAsRead(notificationId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while marking notification as read"));
        }
    }

    @PostMapping("/{userId}/read-all")
    public ResponseEntity<?> markAllNotificationsAsRead(@PathVariable int userId) {
        try {
            notificationService.markAllNotificationsAsRead(userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while marking all notifications as read"));
        }
    }
}
