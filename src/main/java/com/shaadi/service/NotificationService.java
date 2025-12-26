package com.shaadi.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Notification;
import com.shaadi.entity.NotificationType;
import com.shaadi.entity.User;
import com.shaadi.repository.NotificationRepository;
import com.shaadi.repository.UserRepository;

import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notificationRepo;
    private final UserRepository userRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final FCMService fcmService;

    public NotificationService(NotificationRepository notificationRepo, UserRepository userRepo, SimpMessagingTemplate messagingTemplate, FCMService fcmService) {
        this.notificationRepo = notificationRepo;
        this.userRepo = userRepo;
        this.messagingTemplate = messagingTemplate;
        this.fcmService = fcmService;
    }

    public Notification createNotification(NotificationType type, String message, User recipient, User relatedUser) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setRecipient(recipient);
        notification.setRelatedUser(relatedUser);
        Notification savedNotification = notificationRepo.save(notification);

        // Broadcast the notification via WebSocket to the recipient
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedNotification.getRecipient().getId()),
            "/queue/notifications",
            savedNotification
        );

        // Send FCM Notification
        try {
            System.out.println("🔔 Attempting to send FCM notification to user " + recipient.getId() + " (type: " + type + ")");
            if (recipient.getFcmToken() != null && !recipient.getFcmToken().isEmpty()) {
                System.out.println("📱 FCM Token found: " + recipient.getFcmToken().substring(0, Math.min(20, recipient.getFcmToken().length())) + "...");
                String title = "Shaadi App";
                // Customize title based on type if needed
                if (type == NotificationType.REQUEST_RECEIVED) {
                    title = "New Interest Received";
                } else if (type == NotificationType.REQUEST_ACCEPTED) {
                    title = "It's a Match!";
                }

                System.out.println("📤 Sending FCM notification: title='" + title + "', message='" + message + "'");
                fcmService.sendNotification(
                        recipient.getFcmToken(),
                        title,
                        message,
                        java.util.Map.of("type", type.name(), "relatedUserId", String.valueOf(relatedUser.getId()))
                );
                System.out.println("✅ FCM notification sent successfully");
            } else {
                System.out.println("❌ No FCM token found for user " + recipient.getId());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to send FCM notification: " + e.getMessage());
            e.printStackTrace();
        }

        return savedNotification;
    }

    public List<Notification> getNotificationsForUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepo.findByRecipientOrderByCreatedAtDesc(user);
    }

    public List<Notification> getUnreadNotificationsForUser(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepo.findByRecipientAndIsReadOrderByCreatedAtDesc(user, false);
    }

    public void markNotificationAsRead(long notificationId, int userId) {
        notificationRepo.markAsRead(notificationId, userId);
    }

    public void markAllNotificationsAsRead(int userId) {
        notificationRepo.markAllAsRead(userId);
    }

    public long getUnreadNotificationCount(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return notificationRepo.countByRecipientAndIsRead(user, false);
    }

    public void deleteNotification(long notificationId) {
        notificationRepo.deleteById(notificationId);
    }

    public void deleteRequestReceivedNotification(User recipient, User sender) {
        // Find and delete the REQUEST_RECEIVED notification for this specific request
        List<Notification> notifications = notificationRepo.findByRecipientOrderByCreatedAtDesc(recipient);
        for (Notification notification : notifications) {
            if (notification.getType() == NotificationType.REQUEST_RECEIVED &&
                notification.getRelatedUser() != null &&
                notification.getRelatedUser().getId() == sender.getId()) {
                notificationRepo.delete(notification);
                break; // Delete only the first matching notification
            }
        }
    }
}
