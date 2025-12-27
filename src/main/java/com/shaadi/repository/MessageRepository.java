package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.Message;
import com.shaadi.entity.User;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderOrReceiver(User sender, User receiver);

    @Query("SELECT DISTINCT CASE WHEN m.sender.id = :userId THEN m.receiver.id ELSE m.sender.id END FROM Message m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    List<Long> findDistinctChatPartnerIds(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.sender.id = :userId")
    void deleteBySenderId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Message m WHERE m.receiver.id = :userId")
    void deleteByReceiverId(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m LEFT JOIN FETCH m.sender LEFT JOIN FETCH m.receiver WHERE m.sender = :user OR m.receiver = :user")
    List<Message> findBySenderOrReceiverWithUsers(@Param("user") User user);

    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.receiver.id = :receiverId AND m.sender.id = :senderId AND m.read = false")
    void markAsRead(@Param("receiverId") Long receiverId, @Param("senderId") Long senderId);

    @Query("SELECT COUNT(m) > 0 FROM Message m WHERE (m.sender.id = :userId AND m.receiver.id = :otherId) OR (m.sender.id = :otherId AND m.receiver.id = :userId)")
    boolean hasConversationBetween(@Param("userId") Long userId, @Param("otherId") Long otherId);
}
