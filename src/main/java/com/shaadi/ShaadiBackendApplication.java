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
				System.out.println("📄 Checking for Firebase credentials...");

				FirebaseOptions options = null;

				// Method 1: Environment variable (recommended for production)
				String serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT");
				if (serviceAccountJson != null && !serviceAccountJson.isEmpty()) {
					try {
						InputStream serviceAccount = new java.io.ByteArrayInputStream(serviceAccountJson.getBytes());
						options = FirebaseOptions.builder()
								.setCredentials(GoogleCredentials.fromStream(serviceAccount))
								.build();
						System.out.println("✅ Firebase credentials loaded from FIREBASE_SERVICE_ACCOUNT environment variable");
					} catch (Exception e) {
						System.out.println("⚠️ Failed to load from FIREBASE_SERVICE_ACCOUNT: " + e.getMessage());
					}
				}

				// Method 1b: Alternative environment variable name
				if (options == null) {
					String serviceAccountKey = System.getenv("FIREBASE_SERVICE_ACCOUNT_KEY");
					if (serviceAccountKey != null && !serviceAccountKey.isEmpty()) {
						try {
							byte[] decodedKey = java.util.Base64.getDecoder().decode(serviceAccountKey);
							InputStream serviceAccount = new java.io.ByteArrayInputStream(decodedKey);
							options = FirebaseOptions.builder()
									.setCredentials(GoogleCredentials.fromStream(serviceAccount))
									.build();
							System.out.println("✅ Firebase credentials loaded from FIREBASE_SERVICE_ACCOUNT_KEY environment variable");
						} catch (Exception e) {
							System.out.println("⚠️ Failed to decode FIREBASE_SERVICE_ACCOUNT_KEY: " + e.getMessage());
						}
					}
				}

				// Method 2: System property
				if (options == null) {
					String serviceAccountJsonProp = System.getProperty("firebase.service.account.json");
					if (serviceAccountJsonProp != null && !serviceAccountJsonProp.isEmpty()) {
						try {
							InputStream serviceAccount = new java.io.ByteArrayInputStream(serviceAccountJsonProp.getBytes());
							options = FirebaseOptions.builder()
									.setCredentials(GoogleCredentials.fromStream(serviceAccount))
									.build();
							System.out.println("✅ Firebase credentials loaded from system property");
						} catch (Exception e) {
							System.out.println("⚠️ Failed to load from system property: " + e.getMessage());
						}
					}
				}

				// Method 3: File-based (fallback for development)
				if (options == null) {
					try {
						ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
						if (resource.exists()) {
							InputStream serviceAccount = resource.getInputStream();
							options = FirebaseOptions.builder()
									.setCredentials(GoogleCredentials.fromStream(serviceAccount))
									.build();
							System.out.println("✅ Firebase credentials loaded from classpath file");
						}
					} catch (Exception e) {
						System.out.println("⚠️ File-based loading failed: " + e.getMessage());
					}
				}

				if (options != null) {
					System.out.println("🚀 Initializing Firebase app...");
					FirebaseApp.initializeApp(options);
					System.out.println("🎉 Firebase Application Initialized successfully!");
					System.out.println("📱 Available Firebase apps: " + FirebaseApp.getApps().size());
				} else {
					System.err.println("❌ ERROR: No Firebase credentials found. FCM will not work.");
					System.err.println("💡 Set FIREBASE_SERVICE_ACCOUNT_KEY environment variable with base64-encoded service account JSON");
					System.err.println("💡 Or set firebase.service.account.json system property");
					System.err.println("💡 Or ensure firebase-service-account.json is in classpath");
				}
			} else {
				System.out.println("ℹ️ Firebase already initialized");
			}
		} catch (Exception e) {
			System.err.println("❌ Unexpected error initializing Firebase: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
