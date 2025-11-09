package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Plan;
import com.shaadi.entity.Role;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.PlanRepository;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class UserService {
    private final UserRepository userRepo;
    private final PlanRepository planRepo;
    private final SubscriptionRepository subscriptionRepo;
    private final MessageRepository messageRepo;

    public UserService(UserRepository userRepo, PlanRepository planRepo, SubscriptionRepository subscriptionRepo, MessageRepository messageRepo) {
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.messageRepo = messageRepo;
    }

    public User register(User user) {
        // basic check: avoid duplicate email
        Optional<User> existing = userRepo.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }
        // Set default role to USER if not set
        if (user.getRole() == null) {
            user.setRole(Role.USER);
        }
        // Profile fields are null on registration
        return userRepo.save(user);
    }

    public Optional<User> login(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(u -> u.getPassword().equals(password));
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public Optional<User> findById(Integer id) {
        return userRepo.findById(id);
    }

    public User updateUser(User user) {
        // Ensure the user exists
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id required for update");
        }
        return userRepo.save(user);
    }



    public boolean isProfileComplete(User user) {
        return user.getAge() != null && user.getGender() != null &&
               user.getReligion() != null && user.getLocation() != null && user.getBio() != null;
    }

    public List<User> search(int minAge, int maxAge, String location, String religion) {
        return userRepo.findByAgeBetweenAndLocationContainingAndReligionContaining(
                minAge, maxAge,
                (location == null || location.isEmpty()) ? null : location,
                (religion == null || religion.isEmpty()) ? null : religion
        );
    }

    public void deleteUser(Integer id) {
        if (!userRepo.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        // Delete associated messages first to avoid foreign key constraint violations
        messageRepo.deleteBySenderId(id);
        messageRepo.deleteByReceiverId(id);
        // Delete associated subscriptions
        subscriptionRepo.deleteByUserId(id);
        // Now delete the user
        userRepo.deleteById(id);
    }

    public String initiatePasswordReset(String email) {
        Optional<User> user = userRepo.findByEmail(email);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found with email: " + email);
        }
        // Generate a reset token (in a real app, store this with expiration)
        String resetToken = UUID.randomUUID().toString();
        // Here you would typically save the token to the user or a separate table
        // For simplicity, we'll just return it (in production, send via email)
        return resetToken;
    }

    public void resetPassword(String email, String newPassword) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
        user.setPassword(newPassword);
        userRepo.save(user);
    }

    public Subscription purchaseSubscription(Integer userId, Integer planId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();

        // Check for existing active subscription
        Optional<Subscription> existingActive = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(user, SubscriptionStatus.ACTIVE);

        if (existingActive.isPresent() && existingActive.get().getExpiryDate().isAfter(now)) {
            // Extend the existing subscription
            Subscription existing = existingActive.get();
            existing.setExpiryDate(existing.getExpiryDate().plusMonths(plan.getDurationMonths()));
            return subscriptionRepo.save(existing);
        } else {
            // Create new subscription
            LocalDateTime expiry = now.plusMonths(plan.getDurationMonths());
            Subscription subscription = new Subscription();
            subscription.setUser(user);
            subscription.setPlan(plan);
            subscription.setStartDate(now);
            subscription.setExpiryDate(expiry);
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            return subscriptionRepo.save(subscription);
        }
    }
}
