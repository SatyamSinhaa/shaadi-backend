package com.shaadi.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.shaadi.entity.Favourite;
import com.shaadi.entity.Plan;
import com.shaadi.entity.Role;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.BlockRepository;
import com.shaadi.repository.ChatRequestRepository;
import com.shaadi.repository.FavouriteRepository;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.NotificationRepository;
import com.shaadi.repository.PlanRepository;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;
import com.shaadi.dto.SubscriptionResponseDto;
import com.shaadi.service.CloudflareR2Service;

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
    private final ChatRequestRepository chatRequestRepo;
    private final NotificationRepository notificationRepo;
    private final CloudflareR2Service cloudflareR2Service;

    public UserService(UserRepository userRepo, PlanRepository planRepo, SubscriptionRepository subscriptionRepo, MessageRepository messageRepo, FavouriteRepository favouriteRepo, BlockRepository blockRepo, ChatRequestRepository chatRequestRepo, NotificationRepository notificationRepo, CloudflareR2Service cloudflareR2Service) {
        this.userRepo = userRepo;
        this.planRepo = planRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.messageRepo = messageRepo;
        this.favouriteRepo = favouriteRepo;
        this.blockRepo = blockRepo;
        this.chatRequestRepo = chatRequestRepo;
        this.notificationRepo = notificationRepo;
        this.cloudflareR2Service = cloudflareR2Service;
    }

    public Optional<SubscriptionResponseDto> getActiveSubscriptionDtoByUserId(Long userId) {
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

    public List<SubscriptionResponseDto> getSubscriptionHistoryByUserId(Long userId) {
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
        System.out.println("🔥 Starting user registration for: " + user.getEmail());
        try {
            // basic check: avoid duplicate email
            Optional<User> existing = userRepo.findByEmail(user.getEmail());
            if (existing.isPresent()) {
                System.out.println("❌ Email already registered: " + user.getEmail());
                throw new IllegalArgumentException("Email already registered");
            }
            // basic check: avoid duplicate mobile number
            if (user.getMobileNumber() != null) {
                Optional<User> existingMobile = userRepo.findByMobileNumber(user.getMobileNumber());
                if (existingMobile.isPresent()) {
                    System.out.println("❌ Mobile number already registered: " + user.getMobileNumber());
                    throw new IllegalArgumentException("Mobile number already registered");
                }
            }

            // Set default role to USER if not set
            if (user.getRole() == null) {
                user.setRole(Role.USER);
            }

            // Capitalize gender
            if (user.getGender() != null) {
                user.setGender(capitalize(user.getGender()));
                System.out.println("✅ Gender set to: " + user.getGender());
            }

            // Profile fields are null on registration
            System.out.println("💾 Saving user to database...");
            User savedUser = userRepo.save(user);
            System.out.println("✅ User registered successfully with ID: " + savedUser.getId());
            return savedUser;
        } catch (Exception e) {
            System.err.println("❌ Registration failed for " + user.getEmail() + ": " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    // public Optional<User> login(String email, String password) {
    //     return userRepo.findByEmail(email)
    //             .filter(u -> u.getPassword() != null && u.getPassword().equals(password));
    // }

    public User loginWithFirebase(String idToken) throws FirebaseAuthException {
        System.out.println("🔥 Starting Firebase token verification...");
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            System.out.println("✅ Firebase token verified for UID: " + firebaseUid + ", email: " + email);

            // Check if user already exists with this Firebase UID
            Optional<User> existingUser = userRepo.findByFirebaseUid(firebaseUid);
            if (existingUser.isPresent()) {
                System.out.println("✅ Found existing user with Firebase UID: " + firebaseUid);
                return existingUser.get();
            }

            // Check if user exists with same email but different auth method
            if (email != null) {
                Optional<User> emailUser = userRepo.findByEmail(email);
                if (emailUser.isPresent()) {
                    User user = emailUser.get();
                    System.out.println("🔗 Linking Firebase UID to existing user: " + email);
                    // Link Firebase UID to existing user
                    user.setFirebaseUid(firebaseUid);
                    return userRepo.save(user);
                }
            }

            // Don't auto-create user - throw exception to indicate user needs to register
            System.out.println("❌ User not found, needs registration: " + name + " (" + email + ")");
            throw new IllegalArgumentException("USER_NOT_FOUND");
        } catch (FirebaseAuthException e) {
            System.err.println("❌ Firebase token verification failed: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("❌ Unexpected error in Firebase login: " + e.getMessage());
            throw new RuntimeException("Firebase authentication failed: " + e.getMessage());
        }
    }

    public List<User> findAll(String gender) {
        return findAll(gender, null);
    }

    public List<User> findAll(String gender, Long currentUserId) {
        Page<User> page = findAll(gender, currentUserId, PageRequest.of(0, Integer.MAX_VALUE));
        return page.getContent();
    }

    public Page<User> findAll(String gender, Long currentUserId, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        if (gender != null && !gender.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                cb.equal(cb.lower(root.get("gender")), gender.toLowerCase()));
        }

        Page<User> users = userRepo.findAll(spec, pageable);

        if (currentUserId != null) {
            List<User> filteredUsers = users.getContent().stream()
                    .filter(user -> !isBlocked(currentUserId, user.getId()) && !isBlocked(user.getId(), currentUserId))
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(filteredUsers, pageable, filteredUsers.size());
        }

        return users;
    }

    public Optional<User> findById(Long id) {
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
        existing.setFirebaseUid(user.getFirebaseUid()); // Add Firebase UID update
        existing.setAge(user.getAge());
        if (user.getGender() != null) {
            existing.setGender(capitalize(user.getGender()));
        } else {
            existing.setGender(user.getGender());
        }
        
        // Personal Details
        existing.setMaritalStatus(user.getMaritalStatus());
        existing.setManglik(user.getManglik());
        existing.setDateOfBirth(user.getDateOfBirth());
        existing.setHeight(user.getHeight());
        existing.setWeightKg(user.getWeightKg());
        existing.setRashi(user.getRashi());
        existing.setGotr(user.getGotr());
        existing.setCaste(user.getCaste());
        existing.setCategory(user.getCategory());
        existing.setReligion(user.getReligion());
        existing.setProfession(user.getProfession());
        existing.setEducation(user.getEducation());
        existing.setAnnualIncome(user.getAnnualIncome());
        existing.setMotherTongue(user.getMotherTongue());

        // Location
        existing.setCityTown(user.getCityTown());
        existing.setDistrict(user.getDistrict());
        existing.setState(user.getState());

        // Family Details
        existing.setFatherName(user.getFatherName());
        existing.setFatherOccupation(user.getFatherOccupation());
        existing.setMotherName(user.getMotherName());
        existing.setMotherOccupation(user.getMotherOccupation());
        existing.setNumberOfBrothers(user.getNumberOfBrothers());
        existing.setNumberOfSisters(user.getNumberOfSisters());
        existing.setFamilyType(user.getFamilyType());
        existing.setFamilyLocations(user.getFamilyLocations());
        existing.setProperty(user.getProperty());

        // Lifestyle
        existing.setDiet(user.getDiet());
        existing.setSmoking(user.getSmoking());
        existing.setDrinking(user.getDrinking());

        existing.setBio(user.getBio());
        existing.setPhotoUrl(user.getPhotoUrl());

        return userRepo.save(existing);
    }

    public void updateProfilePhoto(Long userId, String photoUrl) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get the old photo URL before updating
        String oldPhotoUrl = user.getPhotoUrl();

        // Update the photo URL in database
        user.setPhotoUrl(photoUrl);
        userRepo.save(user);

        // Delete old photo from Cloudflare R2 if it exists and is different from new URL
        if (oldPhotoUrl != null && !oldPhotoUrl.equals(photoUrl) && !oldPhotoUrl.isBlank()) {
            try {
                String oldFileName = extractFileNameFromUrl(oldPhotoUrl);
                if (oldFileName != null) {
                    System.out.println("Deleting old profile photo: " + oldFileName);
                    cloudflareR2Service.deleteFile(oldFileName);
                    System.out.println("Successfully deleted old profile photo: " + oldFileName);
                } else {
                    System.out.println("Could not extract filename from old photo URL: " + oldPhotoUrl);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete old profile photo: " + e.getMessage());
                // Don't throw exception - photo deletion failure shouldn't break the update
            }
        }
    }

    public void addPhotoToGallery(Long userId, String photoUrl) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        com.shaadi.entity.Photo photo = new com.shaadi.entity.Photo();
        photo.setUser(user);
        photo.setUrl(photoUrl);
        // CreatedAt is handled by default value or @PrePersist if I had it, but the entity has = LocalDateTime.now()
        
        if (user.getPhotos() == null) {
            user.setPhotos(new java.util.ArrayList<>());
        }
        user.getPhotos().add(photo);
        
        userRepo.save(user); // Cascades save to photo
    }

    public void updateFcmToken(Long userId, String token) {
        System.out.println("💾 Updating FCM token for user " + userId + ": " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setFcmToken(token);
        userRepo.save(user);
        System.out.println("✅ FCM token saved successfully for user " + userId);
    }


    public boolean isProfileComplete(User user) {
        return user.getAge() != null && user.getGender() != null &&
               user.getReligion() != null && user.getCityTown() != null && user.getBio() != null;
    }

    public List<User> search(Integer minAge, Integer maxAge, String name, String location, String religion, String gender) {
        return search(minAge, maxAge, name, location, religion, gender, null);
    }

    public List<User> search(Integer minAge, Integer maxAge, String name, String location, String religion, String gender, Long currentUserId) {
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

    public void deleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }

        // Delete associated records in correct order to avoid foreign key violations

        // Delete favourites (both directions)
        favouriteRepo.deleteByUserId(id);
        favouriteRepo.deleteByFavouritedUserId(id);

        // Delete blocks (both directions)
        blockRepo.deleteByBlockerId(id);
        blockRepo.deleteByBlockedId(id);

        // Delete chat requests (both directions)
        chatRequestRepo.deleteBySenderId(id);
        chatRequestRepo.deleteByReceiverId(id);

        // Delete notifications (both directions)
        notificationRepo.deleteByRecipientId(id);
        notificationRepo.deleteByRelatedUserId(id);

        // Delete messages (both directions)
        messageRepo.deleteBySenderId(id);
        messageRepo.deleteByReceiverId(id);

        // Delete subscriptions
        subscriptionRepo.deleteByUserId(id);

        // Finally delete the user (photos and remaining subscriptions will be deleted via cascade)
        userRepo.deleteById(id);
    }



    public Subscription purchaseSubscription(Long userId, Long planId) {
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
        // Extend the existing subscription with slot rollover
        Subscription existing = existingActive.get();

        // Calculate remaining slots
        Integer currentUsed = existing.getUsedChatSlots() != null ? existing.getUsedChatSlots() : 0;
        Integer currentTotal = existing.getChatLimit() != null ? existing.getChatLimit() : 0;
        Integer remainingSlots = Math.max(0, currentTotal - currentUsed);

        // Set new total (rollover remaining + new plan limit)
        existing.setChatLimit(remainingSlots + plan.getChatLimit());

        // Extend expiry
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
        subscription.setUsedChatSlots(0);
        return subscriptionRepo.save(subscription);
    }
}
    }

    public void addFavourite(Long userId, Long favouritedUserId){
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

    public void removeFavourite(Long userId, Long favouritedUserId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User favouritedUser = userRepo.findById(favouritedUserId)
                .orElseThrow(() -> new IllegalArgumentException("Favourited user not found"));

        Favourite favourite = favouriteRepo.findByUserAndFavouritedUser(user, favouritedUser)
                .orElseThrow(() -> new IllegalArgumentException("Favourite not found"));
        favouriteRepo.delete(favourite);
    }

    public List<Favourite> getFavourites(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Favourite> favourites = favouriteRepo.findByUser(user);

        // Filter out blocked users
        return favourites.stream()
                .filter(fav -> !isBlocked(userId, fav.getFavouritedUser().getId()) &&
                              !isBlocked(fav.getFavouritedUser().getId(), userId))
                .toList();
    }

    public void blockUser(Long blockerId, Long blockedId) {
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

    public void unblockUser(Long blockerId, Long blockedId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("Blocker user not found"));
        User blocked = userRepo.findById(blockedId)
                .orElseThrow(() -> new IllegalArgumentException("Blocked user not found"));

        com.shaadi.entity.Block block = blockRepo.findByBlockerAndBlocked(blocker, blocked)
                .orElseThrow(() -> new IllegalArgumentException("Block not found"));
        blockRepo.delete(block);
    }

    public List<com.shaadi.entity.Block> getBlockedUsers(Long blockerId) {
        User blocker = userRepo.findById(blockerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return blockRepo.findByBlocker(blocker);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
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

    private String extractFileNameFromUrl(String url) {
        // Cloudflare R2 URL format: https://account.r2.cloudflarestorage.com/bucket/filename
        // We need to extract "bucket/filename"
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            System.err.println("Failed to parse URL: " + url);
            return null;
        }
    }
}
