package com.shaadi.repository;

import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserAndStatus(User user, SubscriptionStatus status);
    Optional<Subscription> findFirstByUserAndStatusOrderByExpiryDateDesc(User user, SubscriptionStatus status);
    List<Subscription> findByStatusAndExpiryDateBefore(SubscriptionStatus status, java.time.LocalDateTime expiryDate);
    List<Subscription> findByUserOrderByStartDateDesc(User user);

    @Modifying
    @Query("DELETE FROM Subscription s WHERE s.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
