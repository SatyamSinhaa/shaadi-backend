package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import com.shaadi.entity.Favourite;
import com.shaadi.entity.Plan;
import com.shaadi.entity.Role;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.BlockRepository;
import com.shaadi.repository.FavouriteRepository;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.PlanRepository;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;
import com.shaadi.dto.SubscriptionResponseDto;

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
    private final FavouriteRepository favouriteRepo;
    private final BlockRepository blockRepo;

    public UserService(UserRepository userRepo, PlanRepository planRepo, SubscriptionRepository subscriptionRepo, MessageRepository messageRepo, FavouriteRepository favouriteRepo, BlockRepository blockRepo) {
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.messageRepo = messageRepo;
        this.favouriteRepo = favouriteRepo;
        this.blockRepo = blockRepo;
    }

    public Optional<SubscriptionResponseDto> getActiveSubscriptionDtoByUserId(Integer userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();

        Optional<Subscription> subscriptionOpt = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(user, SubscriptionStatus.ACTIVE);

        return subscriptionOpt.map(sub -> {
            SubscriptionResponseDto dto = new SubscriptionResponseDto();
            dto.setSubscriptionId(sub.getId());
            dto.setUserId(user.getId());
            Plan plan = sub.getPlan();
            dto.setPlanId(plan.getId());
            dto.setPlanName(plan.getName());
            dto.setPlanDurationMonths(plan.getDurationMonths());
            dto.setPlanChatLimit(plan.getChatLimit());
            dto.setStartDate(sub.getStartDate());
            dto.setExpiryDate(sub.getExpiryDate());
            dto.setStatus(sub.getStatus().name());
            dto.setChatLimit(sub.getChatLimit());
            return dto;
        });
    }

    public List<SubscriptionResponseDto> getSubscriptionHistoryByUserId(Integer userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        User user = userOpt.get();

        List<Subscription> subscriptions = subscriptionRepo.findByUserOrderByStartDateDesc(user);

        return subscriptions.stream().map(sub -> {
            SubscriptionResponseDto dto = new SubscriptionResponseDto();
            dto.setSubscriptionId(sub.getId());
            dto.setUserId(user.getId());
            Plan plan = sub.getPlan();
            dto.setPlanId(plan.getId());
            dto.setPlanName(plan.getName());
            dto.setPlanDurationMonths(plan.getDurationMonths());
            dto.setPlanChatLimit(plan.getChatLimit());
            dto.setStartDate(sub.getStartDate());
            dto.setExpiryDate(sub.getExpiryDate());
            dto.setStatus(sub.getStatus().name());
            dto.setChatLimit(sub.getChatLimit());
            return dto;
        }).toList();
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
        // Capitalize gender
        if (user.getGender() != null) {
            user.setGender(capitalize(user.getGender()));
        }
        // Profile fields are null on registration
        return userRepo.save(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public Optional<User> login(String email, String password) {
        return userRepo.findByEmail(email)
                .filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    }

    public List<User> findAll(String gender) {
        return findAll(gender, null);
    }

    public List<User> findAll(String gender, Integer currentUserId) {
        List<User> users = userRepo.findAll().stream()
                .filter(user -> gender == null || gender.isEmpty() ||
                        (user.getGender() != null && user.getGender().equalsIgnoreCase(gender)))
                .toList();

        if (currentUserId != null) {
            users = users.stream()
                    .filter(user -> !isBlocked(currentUserId, user.getId()) && !isBlocked(user.getId(), currentUserId))
                    .toList();
        }

        return users;
    }

    public Optional<User> findById(Integer id) {
        return userRepo.findById(id);
    }

    public User updateUser(User user) {
        // Ensure the user exists
        if (user.getId() == null) {
            throw new IllegalArgumentException("User id required for update");
        }
        User existing = userRepo.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Update all provided fields (including null values to clear them)
        // Preserve password and role - don't allow updating these via this endpoint
        existing.setName(user.getName());
        existing.setEmail(user.getEmail());
        existing.setAge(user.getAge());
        if (user.getGender() != null) {
            existing.setGender(capitalize(user.getGender()));
        } else {
            existing.setGender(user.getGender());
        }
        existing.setGotr(user.getGotr());
        existing.setCaste(user.getCaste());
        existing.setCategory(user.getCategory());
        existing.setReligion(user.getReligion());
        existing.setCityTown(user.getCityTown());
        existing.setDistrict(user.getDistrict());
        existing.setState(user.getState());
        existing.setBio(user.getBio());
        existing.setPhotoUrl(user.getPhotoUrl());

        return userRepo.save(existing);
    }


    public boolean isProfileComplete(User user) {
        return user.getAge() != null && user.getGender() != null &&
               user.getReligion() != null && user.getCityTown() != null && user.getBio() != null;
    }

    public List<User> search(Integer minAge, Integer maxAge, String name, String location, String religion, String gender) {
        return search(minAge, maxAge, name, location, religion, gender, null);
    }

    public List<User> search(Integer minAge, Integer maxAge, String name, String location, String religion, String gender, Integer currentUserId) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (minAge != null && maxAge != null) {
            spec = spec.and((root, query, cb) -> cb.between(root.get("age"), minAge, maxAge));
        } else if (minAge != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("age"), minAge));
        } else if (maxAge != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("age"), maxAge));
        }

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), name.toLowerCase() + "%"));
        }

        if (location != null && !location.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("location")), "%" + location.toLowerCase() + "%"));
        }

        if (religion != null && !religion.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("religion")), "%" + religion.toLowerCase() + "%"));
        }

        if (gender != null && !gender.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()));
        }

        List<User> users = userRepo.findAll(spec);

        if (currentUserId != null) {
            users = users.stream()
                    .filter(user -> !isBlocked(currentUserId, user.getId()) && !isBlocked(user.getId(), currentUserId))
                    .toList();
        }

        return users;
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



    public Subscription purchaseSubscription(Integer userId, Integer planId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Plan plan = planRepo.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        LocalDateTime now = LocalDateTime.now();

        // Check for existing active subscription
        Optional<Subscription> existingActive = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(user, SubscriptionStatus.ACTIVE);

if (Boolean.TRUE.equals(plan.getIsAddon())) {
    // Addon plan: extend chat limit without changing expiry
    if (existingActive.isEmpty() || !existingActive.get().getExpiryDate().isAfter(now)) {
        throw new IllegalArgumentException("Addon plans require an active subscription");
    }
    Subscription existing = existingActive.get();
    existing.setChatLimit(existing.getChatLimit() + plan.getChatLimit());
    return subscriptionRepo.save(existing);
} else {
    // Regular plan: extend expiry or create new
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
        subscription.setChatLimit(plan.getChatLimit());
        return subscriptionRepo.save(subscription);
    }
}
    }

    public void addFavourite(Integer userId, Integer favouritedUserId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User favouritedUser = userRepo.findById(favouritedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Favourited user not found"));

        if (userId.equals(favouritedUserId)) {
            throw new IllegalArgumentException("Cannot favourite yourself");
        }

        Optional<Favourite> existing = favouriteRepo.findByUserAndFavouritedUser(user, favouritedUser);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User already favourited");
        }

        Favourite favourite = new Favourite();
        favourite.setUser(user);
        favourite.setFavouritedUser(favouritedUser);
        favouriteRepo.save(favourite);
    }

    public void removeFavourite(Integer userId, Integer favouritedUserId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User favouritedUser = userRepo.findById(favouritedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Favourited user not found"));

        Favourite favourite = favouriteRepo.findByUserAndFavouritedUser(user, favouritedUser)
                .orElseThrow(() -> new IllegalArgumentException("Favourite not found"));
        favouriteRepo.delete(favourite);
    }

    public List<Favourite> getFavourites(Integer userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Favourite> favourites = favouriteRepo.findByUser(user);

        // Filter out blocked users
        return favourites.stream()
                .filter(fav -> !isBlocked(userId, fav.getFavouritedUser().getId()) &&
                              !isBlocked(fav.getFavouritedUser().getId(), userId))
                .toList();
    }

    public void blockUser(Integer blockerId, Integer blockedId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = userRepo.findById(blockedId)
                .orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot block yourself");
        }

        Optional<com.shaadi.entity.Block> existing = blockRepo.findByBlockerAndBlocked(blocker, blocked);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("User already blocked");
        }

        // Remove from favourites if exists
        Optional<Favourite> favourite = favouriteRepo.findByUserAndFavouritedUser(blocker, blocked);
        favourite.ifPresent(favouriteRepo::delete);

        com.shaadi.entity.Block block = new com.shaadi.entity.Block();
        block.setBlocker(blocker);
        block.setBlocked(blocked);
        blockRepo.save(block);
    }

    public void unblockUser(Integer blockerId, Integer blockedId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = userRepo.findById(blockedId)
                .orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        com.shaadi.entity.Block block = blockRepo.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new IllegalArgumentException("Block not found"));
        blockRepo.delete(block);
    }

    public List<com.shaadi.entity.Block> getBlockedUsers(Integer blockerId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return blockRepo.findByBlocker(blocker);
    }

    public boolean isBlocked(Integer blockerId, Integer blockedId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = userRepo.findById(blockedId)
                .orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        return blockRepo.existsByBlockerAndBlocked(blocker, blocked);
    }

    private boolean hasActiveSubscription(User user) {
        LocalDateTime now = LocalDateTime.now();
        return subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(user, SubscriptionStatus.ACTIVE)
                .map(sub -> sub.getExpiryDate().isAfter(now))
                .orElse(false);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
