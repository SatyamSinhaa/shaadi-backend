package com.shaadi.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.shaadi.entity.Message;
import com.shaadi.entity.Plan;
import com.shaadi.entity.Subscription;
import com.shaadi.entity.SubscriptionStatus;
import com.shaadi.entity.User;
import com.shaadi.repository.MessageRepository;
import com.shaadi.repository.SubscriptionRepository;
import com.shaadi.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private MessageRepository messageRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private SubscriptionRepository subscriptionRepo;

    @InjectMocks
    private ChatService chatService;

    @Test
    public void testSendMessageWithActiveSubscription() {
        User sender = new User();
        sender.setId(1);
        User receiver = new User();
        receiver.setId(2);
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("Hello");

        Subscription activeSub = new Subscription();
        activeSub.setExpiryDate(LocalDateTime.now().plusDays(1));
        activeSub.setStatus(SubscriptionStatus.ACTIVE);
        activeSub.setPlan(new Plan());

        when(userRepo.findById(1)).thenReturn(Optional.of(sender));
        when(userRepo.findById(2)).thenReturn(Optional.of(receiver));
        when(subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(sender, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.of(activeSub));
        when(messageRepo.save(message)).thenReturn(message);

        Message result = chatService.sendMessage(message);

        assertNotNull(result);
        assertEquals("Hello", result.getContent());
        verify(messageRepo).save(message);
    }

    @Test
    public void testSendMessageWithExpiredSubscription() {
        User sender = new User();
        sender.setId(1);
        User receiver = new User();
        receiver.setId(2);
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("Hello");

        when(userRepo.findById(1)).thenReturn(Optional.of(sender));
        when(userRepo.findById(2)).thenReturn(Optional.of(receiver));
        when(subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(sender, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            chatService.sendMessage(message);
        });

        assertEquals("Active subscription required to chat", exception.getMessage());
    }

    @Test
    public void testSendMessageWithNoSubscription() {
        User sender = new User();
        sender.setId(1);
        User receiver = new User();
        receiver.setId(2);
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("Hello");

        when(userRepo.findById(1)).thenReturn(Optional.of(sender));
        when(userRepo.findById(2)).thenReturn(Optional.of(receiver));
        when(subscriptionRepo.findFirstByUserAndStatusOrderByExpiryDateDesc(sender, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.empty());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            chatService.sendMessage(message);
        });

        assertEquals("Active subscription required to chat", exception.getMessage());
    }

    @Test
    public void testSendMessageWithNullSender() {
        Message message = new Message();
        message.setReceiver(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(message);
        });

        assertEquals("Sender and receiver must be provided", exception.getMessage());
    }

    @Test
    public void testSendMessageWithNullReceiver() {
        Message message = new Message();
        message.setSender(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessage(message);
        });

        assertEquals("Sender and receiver must be provided", exception.getMessage());
    }

    @Test
    public void testSendMessageAsAdmin() {
        User sender = new User();
        sender.setId(1);
        User receiver = new User();
        receiver.setId(2);
        Message message = new Message();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("Admin message");

        when(messageRepo.save(message)).thenReturn(message);

        Message result = chatService.sendMessageAsAdmin(message);

        assertNotNull(result);
        assertEquals("Admin message", result.getContent());
        verify(messageRepo).save(message);
    }

    @Test
    public void testSendMessageAsAdminWithNullSender() {
        Message message = new Message();
        message.setReceiver(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessageAsAdmin(message);
        });

        assertEquals("Sender and receiver must be provided", exception.getMessage());
    }

    @Test
    public void testSendMessageAsAdminWithNullReceiver() {
        Message message = new Message();
        message.setSender(new User());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            chatService.sendMessageAsAdmin(message);
        });

        assertEquals("Sender and receiver must be provided", exception.getMessage());
    }
}
