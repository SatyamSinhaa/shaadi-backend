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
    public ResponseEntity<Message> sendMessage(@RequestBody Message message) {
        try {
            Message savedMessage = chatService.sendMessage(message);
            return ResponseEntity.ok(savedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Message>> getMessages(@PathVariable int userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<Message> messages = chatService.getMessagesForUser(userOpt.get());
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/message/{id}")
    public ResponseEntity<Message> getMessageById(@PathVariable int id) {
        Optional<Message> messageOpt = chatService.findById(id);
        return messageOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable int id) {
        Optional<Message> messageOpt = chatService.findById(id);
        if (messageOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        chatService.deleteMessage(id);
        return ResponseEntity.noContent().build();
    }
}
