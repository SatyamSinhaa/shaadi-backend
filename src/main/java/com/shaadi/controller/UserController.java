package com.shaadi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.User;
import com.shaadi.entity.Photo;
import com.shaadi.service.UserService;
import com.shaadi.service.EmailService;
import com.shaadi.dto.UserRegistrationDto;
import com.shaadi.dto.ForgotPasswordDto;
import com.shaadi.dto.LoginDto;
import com.shaadi.dto.PurchaseSubscriptionDto;
import com.shaadi.entity.Favourite;
import com.shaadi.entity.Subscription;

import java.util.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {
    private final UserService userService;
    private final EmailService emailService;

    public UserController(UserService userService, EmailService emailService) {
        this.userService = userService;
        this.emailService = emailService;
    }

    @GetMapping("/{userId}/subscription")
    public ResponseEntity<?> getSubscription(@PathVariable Integer userId) {
        Optional<com.shaadi.dto.SubscriptionResponseDto> subscriptionDto = userService.getActiveSubscriptionDtoByUserId(userId);
        if (subscriptionDto.isPresent()) {
            return ResponseEntity.ok(subscriptionDto.get());
        } else {
            return ResponseEntity.status(404).body(Map.of("error", "Active subscription not found"));
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

    @GetMapping
    public ResponseEntity<List<User>> all(@RequestParam(required = false) String gender) {
        try {
            return ResponseEntity.ok(userService.findAll(gender));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id, @RequestBody User user) {
        try {
            user.setId(id); // ensures we update the correct user
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
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
            @RequestParam(required = false) String gender) {
        try {
            return ResponseEntity.ok(userService.search(minAge, maxAge, name, location, religion, gender));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        try {
            String resetToken = userService.initiatePasswordReset(forgotPasswordDto.getEmail());
            emailService.sendPasswordResetEmail(forgotPasswordDto.getEmail(), resetToken);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset email sent");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        try {
            userService.resetPassword(email, newPassword);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password reset successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/purchase-subscription")
    public ResponseEntity<?> purchaseSubscription(@PathVariable Integer userId, @RequestBody PurchaseSubscriptionDto dto) {
        try {
            Subscription subscription = userService.purchaseSubscription(userId, dto.getPlanId());
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{userId}/favourites/{favouritedUserId}")
    public ResponseEntity<?> addFavourite(@PathVariable Integer userId, @PathVariable Integer favouritedUserId) {
        try {
            userService.addFavourite(userId, favouritedUserId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/favourites/{favouritedUserId}")
    public ResponseEntity<?> removeFavourite(@PathVariable Integer userId, @PathVariable Integer favouritedUserId) {
        try {
            userService.removeFavourite(userId, favouritedUserId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/favourites")
    public ResponseEntity<List<Favourite>> getFavourites(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(userService.getFavourites(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @PostMapping("/{userId}/photos")
    public ResponseEntity<?> addPhoto(@PathVariable Integer userId, @RequestBody Map<String, String> payload) {
        try {
            String url = payload.get("url");
            if (url == null || url.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "URL is required"));
            }
            Photo photo = userService.addPhoto(userId, url);
            return ResponseEntity.ok(photo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}/photos/{photoId}")
    public ResponseEntity<?> removePhoto(@PathVariable Integer userId, @PathVariable Integer photoId) {
        try {
            userService.removePhoto(userId, photoId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/photos")
    public ResponseEntity<List<Photo>> getPhotos(@PathVariable Integer userId) {
        try {
            return ResponseEntity.ok(userService.getPhotos(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }
}
