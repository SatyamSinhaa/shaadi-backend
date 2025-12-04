package com.shaadi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.Message;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatService {
    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final SubscriptionRepository subscriptionRepo;

    public ChatService(MessageRepository messageRepo, UserRepository userRepo, SubscriptionRepository subscriptionRepo) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.subscriptionRepo = subscriptionRepo;
    }

    public Message sendMessage(Message message) {
        // Ensure sender and receiver are valid users
        if (message.getSender() == null || message.getReceiver() == null) {
            throw new IllegalArgumentException("Sender and receiver must be provided");
        }

        // Load full sender and receiver entities
        User sender = userRepo.findById(message.getSender().getId())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepo.findById(message.getReceiver().getId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));
        message.setSender(sender);
        message.setReceiver(receiver);

        // Check if sender has an active subscription
        Optional<Subscription> activeSub = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(sender, SubscriptionStatus.ACTIVE);
        if (activeSub.isEmpty() || activeSub.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Active subscription required to chat");
        }
        // Has active subscription, check plan chat limit
        Integer chatLimit = activeSub.get().getPlan().getChatLimit();
        if (chatLimit != null) {
            List<Integer> chatPartnerIds = messageRepo.findDistinctChatPartnerIds(sender.getId());
            if (chatPartnerIds.size() >= chatLimit && !chatPartnerIds.contains(receiver.getId())) {
                throw new IllegalStateException("Chat limit reached for your plan. Upgrade to chat with more users.");
            }
        }

        // Check if receiver has an active subscription
        Optional<Subscription> receiverSub = subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(receiver, SubscriptionStatus.ACTIVE);
        if (receiverSub.isEmpty() || receiverSub.get().getExpiryDate().isBefore(LocalDateTime.now())) {
            // Send default message to non-subscribed receiver
            message.setContent(sender.getName() + " want to send you a message, to start conversation please purchase a plan");
        }

        return messageRepo.save(message);
    }

    public Optional<Message> findById(int id) {
        return messageRepo.findById(id);
    }

    public List<Message> getMessagesForUser(User user) {
        return messageRepo.findBySenderOrReceiverWithUsers(user);
    }

    public void deleteMessage(int id) {
        messageRepo.deleteById(id);
    }

    public Message sendMessageAsAdmin(Message message) {
        // Ensure sender and receiver are valid users
        if (message.getSender() == null || message.getReceiver() == null) {
            throw new IllegalArgumentException("Sender and receiver must be provided");
        }

        // Admin can send messages without subscription checks
        return messageRepo.save(message);
    }

    public void markMessagesAsRead(User receiver, User sender) {
        messageRepo.markAsRead(receiver.getId(), sender.getId());
    }
}
