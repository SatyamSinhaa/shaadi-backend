package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Message;
import com.shaadi.entity.User;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    public ChatService(MessageRepository messageRepo, UserRepository userRepo) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
    }

    public Message sendMessage(Message message) {
        // Ensure sender and receiver are valid users
        if (message.getSender() == null || message.getReceiver() == null) {
            throw new IllegalArgumentException("Sender and receiver must be provided");
        }
        return messageRepo.save(message);
    }

    public Optional<Message> findById(int id) {
        return messageRepo.findById(id);
    }

    public List<Message> getMessagesForUser(User user) {
        return messageRepo.findBySenderOrReceiver(user, user);
    }

    public void deleteMessage(int id) {
        messageRepo.deleteById(id);
    }
}
