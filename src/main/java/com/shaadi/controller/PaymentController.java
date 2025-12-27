package com.shaadi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shaadi.util.PhonePeUtils;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    // Fetch Auth Token API
    @PostMapping("/auth-token")
    public ResponseEntity<Map<String, Object>> fetchAuthToken() {
        try {
            // For Intent SDK, auth token is typically handled differently
            // This is a simplified implementation
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", "AUTH_TOKEN_PLACEHOLDER");
            response.put("expiresAt", System.currentTimeMillis() + 3600000); // 1 hour

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to fetch auth token: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Create Order API for Intent SDK
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        try {
            // Extract parameters from request
            double amount = Double.parseDouble(request.get("amount").toString());
            String userId = request.get("userId").toString();
            String planId = request.getOrDefault("planId", "PLAN_DEFAULT").toString();

            // Generate order ID
            String orderId = "ORDER_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            // Convert amount to paise
            long amountInPaise = (long) (amount * 100);

            // Create order payload for PhonePe Intent SDK
            Map<String, Object> orderPayload = new HashMap<>();
            orderPayload.put("merchantId", PhonePeUtils.getMerchantId());
            orderPayload.put("merchantOrderId", orderId);
            orderPayload.put("merchantUserId", userId);
            orderPayload.put("amount", amountInPaise);
            orderPayload.put("currency", "INR");
            orderPayload.put("description", "Plan Purchase: " + planId);
            orderPayload.put("callbackUrl", "https://your-backend.com/payment/callback");
            orderPayload.put("redirectUrl", "https://your-app.com/payment/success");

            // Convert to JSON
            String jsonPayload = objectMapper.writeValueAsString(orderPayload);

            // For Intent SDK, we generate order token directly
            // In production, you would call PhonePe's create order API
            String orderToken = PhonePeUtils.generateOrderToken(orderId, String.valueOf(amountInPaise), userId);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("orderId", orderId);
            response.put("orderToken", orderToken);
            response.put("amount", amountInPaise);
            response.put("currency", "INR");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create order: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Order Status API
    @GetMapping("/order-status/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable String orderId) {
        try {
            // In production, call PhonePe's order status API
            // For now, return mock response
            Map<String, Object> response = new HashMap<>();
            response.put("orderId", orderId);
            response.put("status", "COMPLETED"); // PENDING, COMPLETED, FAILED
            response.put("amount", 10000); // in paise
            response.put("currency", "INR");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get order status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Legacy PG API endpoint (for backward compatibility)
    @PostMapping("/initiate")
    public ResponseEntity<Map<String, String>> initiatePayment(@RequestBody Map<String, Object> request) {
        try {
            // Extract amount from request
            double amount = Double.parseDouble(request.get("amount").toString());
            // Convert to paise
            long amountInPaise = (long) (amount * 100);

            // Generate transaction ID
            String transactionId = UUID.randomUUID().toString().replace("-", "").substring(0, 34);

            // Assume userId is passed or from security context
            String userId = request.getOrDefault("userId", "USER123").toString();
            String mobileNumber = request.getOrDefault("mobileNumber", "9999999999").toString();

            // Construct the payment payload
            Map<String, Object> paymentPayload = new HashMap<>();
            paymentPayload.put("merchantId", "PGTESTPAYUAT");
            paymentPayload.put("merchantTransactionId", transactionId);
            paymentPayload.put("merchantUserId", userId);
            paymentPayload.put("amount", amountInPaise);
            paymentPayload.put("redirectUrl", "https://webhook.site/redirect-url");
            paymentPayload.put("redirectMode", "REDIRECT");
            paymentPayload.put("callbackUrl", "https://webhook.site/callback-url");
            paymentPayload.put("mobileNumber", mobileNumber);
            paymentPayload.put("paymentInstrument", Map.of("type", "PAY_PAGE"));

            // Convert to JSON string
            String jsonPayload = objectMapper.writeValueAsString(paymentPayload);

            // Base64 encode the JSON
            String base64Body = java.util.Base64.getEncoder().encodeToString(jsonPayload.getBytes());

            // API endpoint for payment initiation
            String apiEndpoint = "/pg/v1/pay";

            // Generate checksum
            String checksum = PhonePeUtils.generateChecksum(base64Body, apiEndpoint);

            // Prepare response
            Map<String, String> response = new HashMap<>();
            response.put("base64Body", base64Body);
            response.put("checksum", checksum);
            response.put("transactionId", transactionId);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to initiate payment: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}
