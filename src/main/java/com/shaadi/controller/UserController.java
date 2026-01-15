package com.shaadi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.User;
import com.shaadi.service.UserService;
import com.shaadi.dto.UserRegistrationDto;
import com.shaadi.dto.LoginDto;
import com.shaadi.dto.PurchaseSubscriptionDto;
import com.shaadi.dto.SubscriptionResponseDto;
import com.shaadi.entity.Favourite;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.Block;
import com.shaadi.service.CloudflareR2Service;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final CloudflareR2Service cloudflareR2Service;

    public UserController(UserService userService, CloudflareR2Service cloudflareR2Service) {
        this.userService = userService;
        this.cloudflareR2Service = cloudflareR2Service;
    }

    @GetMapping("/{userId}/subscription")
    public ResponseEntity<?> getSubscription(@PathVariable Long userId) {
        Optional<com.shaadi.dto.SubscriptionResponseDto> subscriptionDto = userService.getActiveSubscriptionDtoByUserId(userId);
        if (subscriptionDto.isPresent()) {
            return ResponseEntity.ok(subscriptionDto.get());
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Active subscription not found"));
        }
    }

    @GetMapping("/{userId}/subscription-history")
    public ResponseEntity<List<SubscriptionResponseDto>> getSubscriptionHistory(@PathVariable Long userId) {
        try {
            List<SubscriptionResponseDto> history = userService.getSubscriptionHistoryByUserId(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDto registrationDto) {
        try {
            User user = new User();
            user.setEmail(registrationDto.getEmail());
            user.setPassword(registrationDto.getPassword());
            user.setName(registrationDto.getName());
            user.setGender(registrationDto.getGender());
            User savedUser = userService.register(user);
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDto loginDto) {
        Optional<User> userOpt = userService.findByEmail(loginDto.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Email not exist"));
        }
        User user = userOpt.get();
        if (!user.getPassword().equals(loginDto.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Wrong password"));
        }
        return ResponseEntity.ok(user);
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<?> firebaseLogin(@RequestBody Map<String, String> request) {
        try {
            String idToken = request.get("idToken");
            if (idToken == null || idToken.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ID token is required"));
            }
            User user = userService.loginWithFirebase(idToken);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            // Handle USER_NOT_FOUND specifically
            if ("USER_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "USER_NOT_FOUND"));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Firebase authentication failed: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> all(
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Long currentUserId) {
        try {
            return ResponseEntity.ok(userService.findAll(gender, currentUserId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id); // ensures we update the correct user
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/photo")
    public ResponseEntity<?> updateProfilePhoto(@PathVariable Long userId, @RequestBody com.shaadi.dto.PhotoUpdateRequest request) {
        try {
            userService.updateProfilePhoto(userId, request.getPhotoUrl());
            return ResponseEntity.ok(Map.of("message", "Photo updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/gallery")
    public ResponseEntity<?> addPhotoToGallery(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String photoUrl = request.get("photoUrl");
            if (photoUrl == null || photoUrl.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Photo URL is required"));
            }
            userService.addPhotoToGallery(userId, photoUrl);
            return ResponseEntity.ok(Map.of("message", "Photo added to gallery successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/fcm-token")
    public ResponseEntity<?> updateFcmToken(@PathVariable Long userId, @RequestBody Map<String, String> request) {
        try {
            String token = request.get("token");
            if (token == null || token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token is required"));
            }
            userService.updateFcmToken(userId, token);
            return ResponseEntity.ok(Map.of("message", "FCM Token updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }



    @GetMapping("/search")
    public ResponseEntity<List<User>> search(
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String gender,
            @RequestParam Long currentUserId) {
        try {
            return ResponseEntity.ok(userService.search(minAge, maxAge, name, location, religion, gender, currentUserId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }


    @PostMapping("/{userId}/purchase-subscription")
    public ResponseEntity<?> purchaseSubscription(@PathVariable Long userId, @RequestBody PurchaseSubscriptionDto dto) {
        try {
            Subscription subscription = userService.purchaseSubscription(userId, dto.getPlanId());
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/favourites/{favouritedUserId}")
    public ResponseEntity<?> addFavourite(@PathVariable Long userId, @PathVariable Long favouritedUserId) {
        try {
            userService.addFavourite(userId, favouritedUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/favourites/{favouritedUserId}")
    public ResponseEntity<?> removeFavourite(@PathVariable Long userId, @PathVariable Long favouritedUserId) {
        try {
            userService.removeFavourite(userId, favouritedUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/favourites")
    public ResponseEntity<List<Favourite>> getFavourites(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(userService.getFavourites(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PostMapping("/{blockerId}/block/{blockedId}")
    public ResponseEntity<?> blockUser(@PathVariable Long blockerId, @PathVariable Long blockedId) {
        try {
            userService.blockUser(blockerId, blockedId);
            return ResponseEntity.ok(Map.of("message", "User blocked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{blockerId}/block/{blockedId}")
    public ResponseEntity<?> unblockUser(@PathVariable Long blockerId, @PathVariable Long blockedId) {
        try {
            userService.unblockUser(blockerId, blockedId);
            return ResponseEntity.ok(Map.of("message", "User unblocked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{blockerId}/blocked")
    public ResponseEntity<List<Block>> getBlockedUsers(@PathVariable Long blockerId) {
        try {
            return ResponseEntity.ok(userService.getBlockedUsers(blockerId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PostMapping("/upload-url")
    public ResponseEntity<?> getUploadUrl(@RequestBody com.shaadi.dto.UploadUrlRequest request) {
        try {
            System.out.println("Upload URL request received: " + request);
            String fileName = request.getFileName();
            String contentType = request.getContentType();

            System.out.println("FileName: " + fileName + ", ContentType: " + contentType);

            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File name is required"));
            }
            if (contentType == null || contentType.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Content type is required"));
            }

            Map<String, String> urls = cloudflareR2Service.generateUploadUrl(fileName, contentType);
            System.out.println("Generated URLs: " + urls);
            return ResponseEntity.ok(urls);
        } catch (Exception e) {
            System.out.println("Error generating upload URL: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate upload URL: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete-file")
    public ResponseEntity<?> deleteFile(@RequestBody Map<String, String> request) {
        try {
            String fileName = request.get("fileName");
            if (fileName == null || fileName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File name is required"));
            }

            cloudflareR2Service.deleteFile(fileName);
            return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to delete file: " + e.getMessage()));
        }
    }


}
