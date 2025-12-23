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
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
                // Only initialize if file exists. If not, log warning (user needs to add it)
                if (resource.exists()) {
                    InputStream serviceAccount = resource.getInputStream();

                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase Application Initialized");
                } else {
                    System.out.println("WARNING: firebase-service-account.json not found in resources. FCM will not work.");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
