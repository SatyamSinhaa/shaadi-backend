package com.shaadi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.User;
import com.shaadi.service.UserService;
import com.shaadi.service.EmailService;
import com.shaadi.dto.UserRegistrationDto;
import com.shaadi.dto.ForgotPasswordDto;
import com.shaadi.dto.LoginDto;
import com.shaadi.dto.PurchaseSubscriptionDto;
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

    @PostMapping("/register")
    public User register(@RequestBody UserRegistrationDto registrationDto) {
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setPassword(registrationDto.getPassword());
        user.setName(registrationDto.getName());
        return userService.register(user);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody LoginDto loginDto) {
        Optional<User> loggedInUser = userService.login(loginDto.getEmail(), loginDto.getPassword());
        if (loggedInUser.isPresent()) {
            return ResponseEntity.ok(loggedInUser.get());
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @GetMapping
    public List<User> all() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<User> getById(@PathVariable Integer id) {
        return userService.findById(id);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable int id, @RequestBody User user) {
        user.setId(id); // ensures we update the correct user
        return userService.updateUser(user);
    }



    @GetMapping("/search")
    public List<User> search(
            @RequestParam(defaultValue = "18") int minAge,
            @RequestParam(defaultValue = "60") int maxAge,
            @RequestParam(defaultValue = "") String location,
            @RequestParam(defaultValue = "") String religion) {
        return userService.search(minAge, maxAge, location, religion);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/forgot-password")
    public Map<String, String> forgotPassword(@RequestBody ForgotPasswordDto forgotPasswordDto) {
        String resetToken = userService.initiatePasswordReset(forgotPasswordDto.getEmail());
        emailService.sendPasswordResetEmail(forgotPasswordDto.getEmail(), resetToken);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset email sent");
        return response;
    }

    @PostMapping("/reset-password")
    public Map<String, String> resetPassword(@RequestParam String email, @RequestParam String newPassword) {
        userService.resetPassword(email, newPassword);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successfully");
        return response;
    }

    @PostMapping("/{userId}/purchase-subscription")
    public ResponseEntity<Subscription> purchaseSubscription(@PathVariable Integer userId, @RequestBody PurchaseSubscriptionDto dto) {
        try {
            Subscription subscription = userService.purchaseSubscription(userId, dto.getPlanId());
            return ResponseEntity.ok(subscription);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
