package com.shaadi.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.entity.Plan;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;
import com.shaadi.repository.PlanRepository;
import com.shaadi.repository.PhotoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepo;
    private final UserRepository userRepo;
    private final PlanRepository planRepo;
    private final PhotoRepository photoRepo;

    public SubscriptionService(SubscriptionRepository subscriptionRepo, UserRepository userRepo, PlanRepository planRepo, PhotoRepository photoRepo) {
        this.subscriptionRepo = subscriptionRepo;
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.photoRepo = photoRepo;
    }

    // Method to update expired subscriptions
    public void updateExpiredSubscriptions() {
        List<Subscription> expiredSubs = subscriptionRepo.findByStatusAndExpiryDateBefore(SubscriptionStatus.ACTIVE, LocalDateTime.now());
        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepo.save(sub);
            // Remove excess photos when subscription expires
            removeExcessPhotos(sub.getUser());
        }
    }

    // Scheduled job to run daily at midnight
    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledExpiryCheck() {
        updateExpiredSubscriptions();
    }

    public List<Subscription> findAll() {
        return subscriptionRepo.findAll();
    }

    public Subscription giveSubscription(Integer userId, Integer planId, Integer durationMonths) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();
        Integer actualDuration = durationMonths != null ? durationMonths : plan.getDurationMonths();

        // Check for existing active subscription
        Optional<Subscription> existingActive = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(user, SubscriptionStatus.ACTIVE);

        if (existingActive.isPresent() && existingActive.get().getExpiryDate().isAfter(now)) {
            // Extend the existing subscription
            Subscription existing = existingActive.get();
            existing.setExpiryDate(existing.getExpiryDate().plusMonths(actualDuration));
            return subscriptionRepo.save(existing);
        } else {
            // Create new subscription
            LocalDateTime expiry = now.plusMonths(actualDuration);
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStartDate(now);
            subscription.setExpiryDate(expiry);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            return subscriptionRepo.save(subscription);
        }
    }

    public void revokeSubscription(Integer userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Subscription> activeSubscriptions = subscriptionRepo.findByUserAndStatus(user, SubscriptionStatus.ACTIVE);
        for (Subscription sub : activeSubscriptions) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepo.save(sub);
            // Remove excess photos when subscription is revoked
            removeExcessPhotos(user);
        }
    }

    private void removeExcessPhotos(User user) {
        List<com.shaadi.entity.Photo> photos = photoRepo.findByUser(user);
        int maxPhotos = 1; // Default for non-subscribers
        if (photos.size() > maxPhotos) {
            // Remove excess photos, keeping only the first one
            for (int i = maxPhotos; i < photos.size(); i++) {
                photoRepo.delete(photos.get(i));
            }
        }
    }
}
