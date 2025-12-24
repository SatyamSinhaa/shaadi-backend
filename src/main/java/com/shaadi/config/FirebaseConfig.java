package com.shaadi.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            System.out.println("🔥 Starting Firebase initialization...");
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                System.out.println("📄 Firebase not initialized yet, checking for service account file...");
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                // Only initialize if file exists. If not, log warning (user needs to add it)
                if (resource.exists()) {
                    System.out.println("✅ firebase-service-account.json found, initializing Firebase...");
                    InputStream serviceAccount = resource.getInputStream();

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("🎉 Firebase Application Initialized successfully!");
                    System.out.println("📱 Available Firebase apps: " + FirebaseApp.getApps().size());
                } else {
                    System.out.println("❌ WARNING: firebase-service-account.json not found in resources. FCM will not work.");
                }
            } else {
                System.out.println("ℹ️ Firebase already initialized");
            }
        } catch (IOException e) {
            System.err.println("❌ Error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("❌ Unexpected error initializing Firebase: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
