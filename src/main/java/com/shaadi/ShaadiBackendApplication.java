package com.shaadi;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.io.InputStream;

@SpringBootApplication
@EnableScheduling
public class ShaadiBackendApplication {

	public static void main(String[] args) {
		// Initialize Firebase before Spring Boot starts
		initializeFirebase();
		SpringApplication.run(ShaadiBackendApplication.class, args);
	}

	private static void initializeFirebase() {
		try {
			System.out.println("🔥 Initializing Firebase in main method...");
			if (FirebaseApp.getApps().isEmpty()) {
				System.out.println("📄 Firebase not initialized yet, checking for service account file...");
				ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

				if (resource.exists()) {
					System.out.println("✅ firebase-service-account.json found, initializing Firebase...");
					InputStream serviceAccount = resource.getInputStream();

					FirebaseOptions options = FirebaseOptions.builder()
							.setCredentials(GoogleCredentials.fromStream(serviceAccount))
							.build();

					FirebaseApp.initializeApp(options);
					System.out.println("🎉 Firebase Application Initialized successfully in main method!");
					System.out.println("📱 Available Firebase apps: " + FirebaseApp.getApps().size());
				} else {
					System.err.println("❌ WARNING: firebase-service-account.json not found in resources. FCM will not work.");
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
