package com.shaadi.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

public class PhonePeUtils {

    // PhonePe Intent SDK credentials - replace with actual values
    private static final String SALT_KEY = "099eb0cd-02cf-4e2a-8aca-3e6c6aff0399";
    private static final String SALT_INDEX = "1";
    private static final String MERCHANT_ID = "PGTESTPAYUAT";
    private static final String BASE_URL_SANDBOX = "https://api-preprod.phonepe.com/apis/pg-sandbox";
    private static final String BASE_URL_PRODUCTION = "https://api.phonepe.com/apis/pg";

    public static String generateChecksum(String base64Body, String apiEndpoint) throws NoSuchAlgorithmException {
        String dataToHash = base64Body + apiEndpoint + SALT_KEY;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
        String checksum = bytesToHex(hash) + "###" + SALT_INDEX;
        return checksum;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    // Generate X-VERIFY header for Intent SDK APIs
    public static String generateXVerify(String requestBody, String endpoint) throws NoSuchAlgorithmException {
        String dataToHash = requestBody + endpoint + SALT_KEY;
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash) + "###" + SALT_INDEX;
    }

    // Generate order token for Intent SDK
    public static String generateOrderToken(String orderId, String amount, String userId) {
        // This is a simplified version - in production, this should be a proper JWT or secure token
        return Base64.getEncoder().encodeToString(
            (orderId + "|" + amount + "|" + userId + "|" + System.currentTimeMillis())
                .getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String getMerchantId() {
        return MERCHANT_ID;
    }

    public static String getSaltKey() {
        return SALT_KEY;
    }

    public static String getSaltIndex() {
        return SALT_INDEX;
    }

    public static String getBaseUrl(boolean isProduction) {
        return isProduction ? BASE_URL_PRODUCTION : BASE_URL_SANDBOX;
    }
}
