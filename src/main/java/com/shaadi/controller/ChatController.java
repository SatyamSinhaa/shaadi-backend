package com.shaadi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.Message;
import com.shaadi.entity.User;
import com.shaadi.service.ChatService;
import com.shaadi.repository.UserRepository;

import java.util.*;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {
    private final ChatService chatService;
    private final UserRepository userRepo;

    public ChatController(ChatService chatService, UserRepository userRepo) {
        this.chatService = chatService;
        this.userRepo = userRepo;
    }

    @PostMapping
    public ResponseEntity<?> sendMessage(@RequestBody Message message) {
        try {
            Message savedMessage = chatService.sendMessage(message);
            return ResponseEntity.ok(savedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage())); // Forbidden for no active subscription
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getMessages(@PathVariable int userId) {
        try {
            Optional<User> userOpt = userRepo.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            List<Message> messages = chatService.getMessagesForUser(userOpt.get());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving messages"));
        }
    }

    @GetMapping("/message/{id}")
    public ResponseEntity<?> getMessageById(@PathVariable int id) {
        try {
            Optional<Message> messageOpt = chatService.findById(id);
            if (messageOpt.isPresent()) {
                return ResponseEntity.ok(messageOpt.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "Message not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving the message"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable int id) {
        try {
            Optional<Message> messageOpt = chatService.findById(id);
            if (messageOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Message not found"));
            }
            chatService.deleteMessage(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while deleting the message"));
        }
    }

    @PostMapping("/mark-read/{receiverId}/{senderId}")
    public ResponseEntity<?> markMessagesAsRead(@PathVariable int receiverId, @PathVariable int senderId) {
        try {
            Optional<User> receiverOpt = userRepo.findById(receiverId);
            Optional<User> senderOpt = userRepo.findById(senderId);
            if (receiverOpt.isEmpty() || senderOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            chatService.markMessagesAsRead(receiverOpt.get(), senderOpt.get());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while marking messages as read"));
        }
    }
}
