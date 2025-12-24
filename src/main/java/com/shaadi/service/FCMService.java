package com.shaadi.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class FCMService {

    public void sendNotification(String token, String title, String body, Map<String, String> data) {
        if (token == null || token.isEmpty()) {
            System.out.println("❌ FCM token is null or empty");
            return;
        }

        try {
            System.out.println("🔥 Building FCM message for token: " + token.substring(0, Math.min(20, token.length())) + "...");

            com.google.firebase.messaging.Notification notification = com.google.firebase.messaging.Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(token)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
                System.out.println("📊 Adding data to message: " + data);
            }

            Message message = messageBuilder.build();
            System.out.println("📨 Sending FCM message...");
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Successfully sent FCM message: " + response);
        } catch (Exception e) {
            System.err.println("❌ Error sending FCM notification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
