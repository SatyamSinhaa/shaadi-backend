package com.shaadi.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.shaadi.entity.ChatRequest;
import com.shaadi.entity.Message;
import com.shaadi.entity.Notification;

@Controller
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload Message message) {
        // Broadcast the message to the receiver
        messagingTemplate.convertAndSendToUser(
            String.valueOf(message.getReceiver().getId()),
            "/queue/messages",
            message
        );

        // Also broadcast to sender to confirm delivery
        messagingTemplate.convertAndSendToUser(
            String.valueOf(message.getSender().getId()),
            "/queue/messages",
            message
        );
    }

    @MessageMapping("/chat.sendRequest")
    public void sendChatRequest(@Payload ChatRequest chatRequest) {
        // Broadcast the chat request to the receiver
        messagingTemplate.convertAndSendToUser(
            String.valueOf(chatRequest.getReceiver().getId()),
            "/queue/chatRequests",
            chatRequest
        );

        // Also broadcast to sender to confirm sending
        messagingTemplate.convertAndSendToUser(
            String.valueOf(chatRequest.getSender().getId()),
            "/queue/chatRequests",
            chatRequest
        );
    }

    @MessageMapping("/chat.sendNotification")
    public void sendNotification(@Payload Notification notification) {
        // Broadcast the notification to the target user
        messagingTemplate.convertAndSendToUser(
            String.valueOf(notification.getRecipient().getId()),
            "/queue/notifications",
            notification
        );
    }

    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload MarkAsReadRequest request) {
        // Broadcast read status update to both users
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getSenderId()),
            "/queue/read",
            request
        );
        messagingTemplate.convertAndSendToUser(
            String.valueOf(request.getReceiverId()),
            "/queue/read",
            request
        );
    }

    public static class MarkAsReadRequest {
        private int senderId;
        private int receiverId;

        public MarkAsReadRequest() {}

        public MarkAsReadRequest(int senderId, int receiverId) {
            this.senderId = senderId;
            this.receiverId = receiverId;
        }

        public int getSenderId() { return senderId; }
        public void setSenderId(int senderId) { this.senderId = senderId; }

        public int getReceiverId() { return receiverId; }
        public void setReceiverId(int receiverId) { this.receiverId = receiverId; }
    }
}
