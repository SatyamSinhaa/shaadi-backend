package com.shaadi.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shaadi.entity.ChatRequest;
import com.shaadi.entity.ChatRequestStatus;
import com.shaadi.entity.Message;
import com.shaadi.entity.Notification;
import com.shaadi.entity.NotificationType;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.ChatRequestRepository;
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
    private final ChatRequestRepository chatRequestRepo;
    private final NotificationService notificationService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(MessageRepository messageRepo, UserRepository userRepo, SubscriptionRepository subscriptionRepo, ChatRequestRepository chatRequestRepo, NotificationService notificationService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.chatRequestRepo = chatRequestRepo;
        this.notificationService = notificationService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
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

        // Check if chat request is accepted
        if (!canChat(sender.getId(), receiver.getId())) {
            throw new IllegalStateException("Chat request not accepted");
        }

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

        Message savedMessage = messageRepo.save(message);

        // Broadcast the message via WebSocket to both sender and receiver
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedMessage.getReceiver().getId()),
            "/queue/messages",
            savedMessage
        );
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedMessage.getSender().getId()),
            "/queue/messages",
            savedMessage
        );

        return savedMessage;
    }

    public Optional<Message> findById(int id) {
        return messageRepo.findById(id);
    }

    public List<Message> getMessagesForUser(User user) {
        List<Message> messages = messageRepo.findBySenderOrReceiverWithUsers(user);

        // Filter out messages from/to blocked users
        return messages.stream()
                .filter(message -> {
                    Integer otherUserId = message.getSender().getId().equals(user.getId()) ?
                            message.getReceiver().getId() : message.getSender().getId();
                    return !userService.isBlocked(user.getId(), otherUserId) &&
                           !userService.isBlocked(otherUserId, user.getId());
                })
                .toList();
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

    public ChatRequest sendRequest(int senderId, int receiverId) {
        if (senderId == receiverId) {
            throw new IllegalArgumentException("Cannot send request to yourself");
        }

        User sender = userRepo.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepo.findById(receiverId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        // Check if request already exists
        List<ChatRequest> existingRequests = chatRequestRepo.findRequestsBetweenUsers(sender, receiver);
        if (!existingRequests.isEmpty()) {
            throw new IllegalStateException("Chat request already exists between these users");
        }

        ChatRequest request = new ChatRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setStatus(ChatRequestStatus.PENDING);

        ChatRequest savedRequest = chatRequestRepo.save(request);

        // Broadcast the chat request via WebSocket to both users
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedRequest.getReceiver().getId()),
            "/queue/chatRequests",
            savedRequest
        );
        messagingTemplate.convertAndSendToUser(
            String.valueOf(savedRequest.getSender().getId()),
            "/queue/chatRequests",
            savedRequest
        );

        // Create notification for the receiver
        notificationService.createNotification(
            NotificationType.REQUEST_RECEIVED,
            sender.getName() + " sends a request",
            receiver,
            sender
        );

        // Create notification for the sender (so they know they sent the request)
        notificationService.createNotification(
            NotificationType.REQUEST_RECEIVED,
            "You sent a request to " + receiver.getName(),
            sender,
            receiver
        );

        return savedRequest;
    }

    public ChatRequest acceptRequest(long requestId, int userId) {
        ChatRequest request = chatRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getReceiver().getId() != userId) {
            throw new IllegalArgumentException("You can only accept requests sent to you");
        }

        if (request.getStatus() != ChatRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        request.setStatus(ChatRequestStatus.ACCEPTED);
        ChatRequest savedRequest = chatRequestRepo.save(request);

        // Create notification for the sender
        notificationService.createNotification(
            NotificationType.REQUEST_ACCEPTED,
            request.getReceiver().getName() + " accepted your request",
            request.getSender(),
            request.getReceiver()
        );

        return savedRequest;
    }

    public void rejectRequest(long requestId, int userId) {
        ChatRequest request = chatRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getReceiver().getId() != userId) {
            throw new IllegalArgumentException("You can only reject requests sent to you");
        }

        if (request.getStatus() != ChatRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        // Delete the REQUEST_RECEIVED notification for the receiver
        notificationService.deleteRequestReceivedNotification(request.getReceiver(), request.getSender());

        // Create notification for the sender before deleting the request
        notificationService.createNotification(
            NotificationType.REQUEST_REJECTED,
            request.getReceiver().getName() + " rejected your chat request",
            request.getSender(),
            request.getReceiver()
        );

        chatRequestRepo.delete(request);
    }

    public void cancelRequest(long requestId, int userId) {
        ChatRequest request = chatRequestRepo.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.getSender().getId() != userId) {
            throw new IllegalArgumentException("You can only cancel your own requests");
        }

        if (request.getStatus() != ChatRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not pending");
        }

        // Delete the REQUEST_RECEIVED notification for the receiver
        notificationService.deleteRequestReceivedNotification(request.getReceiver(), request.getSender());

        // Delete the REQUEST_RECEIVED notification for the sender (their own notification)
        notificationService.deleteRequestReceivedNotification(request.getSender(), request.getReceiver());

        chatRequestRepo.delete(request);
    }

    public List<ChatRequest> getPendingRequestsForUser(int userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return chatRequestRepo.findByReceiverAndStatus(user, ChatRequestStatus.PENDING);
    }

    public List<ChatRequest> getAllRequestsForUser(int userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ChatRequest> requests = chatRequestRepo.findBySenderOrReceiver(user);

        // Filter out requests from/to blocked users
        return requests.stream()
                .filter(request -> {
                    Integer otherUserId = request.getSender().getId().equals(userId) ?
                            request.getReceiver().getId() : request.getSender().getId();
                    return !userService.isBlocked(userId, otherUserId) &&
                           !userService.isBlocked(otherUserId, userId);
                })
                .toList();
    }

    public boolean canChat(int userId1, int userId2) {
        User user1 = userRepo.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        User user2 = userRepo.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<ChatRequest> acceptedRequests = chatRequestRepo.findRequestsBetweenUsersWithStatus(user1, user2, ChatRequestStatus.ACCEPTED);
        return !acceptedRequests.isEmpty();
    }
}
