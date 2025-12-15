package com.shaadi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shaadi.entity.ChatRequest;
import com.shaadi.entity.ChatRequestStatus;
import com.shaadi.entity.User;

import java.util.List;
import java.util.Optional;

public interface ChatRequestRepository extends JpaRepository<ChatRequest, Long> {
    Optional<ChatRequest> findBySenderAndReceiver(User sender, User receiver);

    @Query("SELECT cr FROM ChatRequest cr WHERE cr.receiver = :receiver AND cr.status = :status")
    List<ChatRequest> findByReceiverAndStatus(@Param("receiver") User receiver, @Param("status") ChatRequestStatus status);

    @Query("SELECT cr FROM ChatRequest cr WHERE (cr.sender = :user1 AND cr.receiver = :user2) OR (cr.sender = :user2 AND cr.receiver = :user1)")
    List<ChatRequest> findRequestsBetweenUsers(@Param("user1") User user1, @Param("user2") User user2);

    @Query("SELECT cr FROM ChatRequest cr WHERE ((cr.sender = :user1 AND cr.receiver = :user2) OR (cr.sender = :user2 AND cr.receiver = :user1)) AND cr.status = :status")
    List<ChatRequest> findRequestsBetweenUsersWithStatus(@Param("user1") User user1, @Param("user2") User user2, @Param("status") ChatRequestStatus status);

    @Query("SELECT cr FROM ChatRequest cr WHERE cr.sender = :user OR cr.receiver = :user")
    List<ChatRequest> findBySenderOrReceiver(@Param("user") User user);

    @Query("SELECT cr FROM ChatRequest cr WHERE (cr.sender.id = :userId1 AND cr.receiver.id = :userId2) OR (cr.sender.id = :userId2 AND cr.receiver.id = :userId1)")
    List<ChatRequest> findRequestsBetweenUsersByIds(@Param("userId1") int userId1, @Param("userId2") int userId2);
}
