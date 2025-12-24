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

				// Try multiple ways to load the file
				InputStream serviceAccount = null;

				// Method 1: ClassPathResource
				try {
					ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
					if (resource.exists()) {
						serviceAccount = resource.getInputStream();
						System.out.println("✅ firebase-service-account.json found via ClassPathResource");
					}
				} catch (Exception e) {
					System.out.println("⚠️ ClassPathResource failed: " + e.getMessage());
				}

				// Method 2: Direct file path (fallback)
				if (serviceAccount == null) {
					try {
						java.io.File file = new java.io.File("src/main/resources/firebase-service-account.json");
						if (file.exists()) {
							serviceAccount = new java.io.FileInputStream(file);
							System.out.println("✅ firebase-service-account.json found via direct file path");
						}
					} catch (Exception e) {
						System.out.println("⚠️ Direct file path failed: " + e.getMessage());
					}
				}

				// Method 3: From classpath as stream (for JAR deployment)
				if (serviceAccount == null) {
					try {
						serviceAccount = ShaadiBackendApplication.class.getClassLoader().getResourceAsStream("firebase-service-account.json");
						if (serviceAccount != null) {
							System.out.println("✅ firebase-service-account.json found via ClassLoader");
						}
					} catch (Exception e) {
						System.out.println("⚠️ ClassLoader failed: " + e.getMessage());
					}
				}

				// Method 4: Try BOOT-INF/classes path (Spring Boot JAR structure)
				if (serviceAccount == null) {
					try {
						serviceAccount = ShaadiBackendApplication.class.getClassLoader().getResourceAsStream("BOOT-INF/classes/firebase-service-account.json");
						if (serviceAccount != null) {
							System.out.println("✅ firebase-service-account.json found via BOOT-INF/classes/ path");
						}
					} catch (Exception e) {
						System.out.println("⚠️ BOOT-INF/classes path failed: " + e.getMessage());
					}
				}

				if (serviceAccount != null) {
					System.out.println("🔧 Building Firebase options...");
					FirebaseOptions options = FirebaseOptions.builder()
							.setCredentials(GoogleCredentials.fromStream(serviceAccount))
							.build();

					System.out.println("🚀 Initializing Firebase app...");
					FirebaseApp.initializeApp(options);
					System.out.println("🎉 Firebase Application Initialized successfully in main method!");
					System.out.println("📱 Available Firebase apps: " + FirebaseApp.getApps().size());

					serviceAccount.close();
				} else {
					System.err.println("❌ ERROR: firebase-service-account.json not found by any method. FCM will not work.");
					System.err.println("💡 Make sure the file is in src/main/resources/ and is properly included in the JAR");
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
