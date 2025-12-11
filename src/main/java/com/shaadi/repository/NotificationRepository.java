package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.Notification;
import com.shaadi.entity.User;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.recipient LEFT JOIN FETCH n.relatedUser WHERE n.recipient = :recipient ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientOrderByCreatedAtDesc(User recipient);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.recipient LEFT JOIN FETCH n.relatedUser WHERE n.recipient = :recipient AND n.isRead = :isRead ORDER BY n.createdAt DESC")
    List<Notification> findByRecipientAndIsReadOrderByCreatedAtDesc(User recipient, @Param("isRead") Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.recipient.id = :recipientId AND n.isRead = false")
    void markAllAsRead(@Param("recipientId") Integer recipientId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :notificationId AND n.recipient.id = :recipientId")
    void markAsRead(@Param("notificationId") Long notificationId, @Param("recipientId") Integer recipientId);

    long countByRecipientAndIsRead(User recipient, Boolean isRead);
}
