package com.shaadi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.shaadi.entity.Role;
import com.shaadi.entity.User;
import com.shaadi.entity.Message;
import com.shaadi.entity.Plan;
import com.shaadi.entity.Subscription;
import com.shaadi.service.UserService;
import com.shaadi.service.PlanService;
import com.shaadi.service.SubscriptionService;
import com.shaadi.service.ChatService;
import com.shaadi.dto.AdminGiveSubscriptionDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserService userService;
    private final PlanService planService;
    private final SubscriptionService subscriptionService;
    private final ChatService chatService;

    public AdminController(UserService userService, PlanService planService, SubscriptionService subscriptionService, ChatService chatService) {
        this.userService = userService;
        this.planService = planService;
        this.subscriptionService = subscriptionService;
        this.chatService = chatService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalUsers = userService.findAll(null).size();
        long totalAdmins = userService.findAll(null).stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long totalSubscriptions = subscriptionService.findAll().size();
        model.addAttribute("stats", Map.of(
            "totalUsers", totalUsers,
            "totalAdmins", totalAdmins,
            "totalSubscriptions", totalSubscriptions
        ));
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String getAllUsers(Model model) {
        model.addAttribute("users", userService.findAll(null));
        return "admin/users";
    }

    @GetMapping("/users/{id}")
    @ResponseBody
    public ResponseEntity<?> getUserById(@PathVariable Integer id) {
        try {
            Optional<User> userOpt = userService.findById(id);
            if (userOpt.isPresent()) {
                return ResponseEntity.ok(userOpt.get());
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving the user"));
        }
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Integer id, Model model) {
        userService.findById(id).ifPresent(user -> model.addAttribute("user", user));
        return "admin/user-edit";
    }

    @PostMapping("/users/{id}")
    public String updateUser(@PathVariable Integer id, @ModelAttribute User user) {
        try {
            user.setId(id);
            userService.updateUser(user);
            return "redirect:/admin/users";
        } catch (Exception e) {
            // For view-based endpoints, we can't return JSON, so redirect with error param or handle differently
            return "redirect:/admin/users?error=" + e.getMessage();
        }
    }

    @PostMapping("/users/{id}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Integer id) {
        try {
            userService.deleteUser(id);
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "User deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while deleting the user"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // API endpoints for JSON responses (optional)
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<User>> getAllUsersApi() {
        try {
            return ResponseEntity.ok(userService.findAll(null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatsApi() {
        try {
            long totalUsers = userService.findAll(null).size();
            long totalAdmins = userService.findAll(null).stream().filter(u -> u.getRole() == Role.ADMIN).count();
            long totalSubscriptions = subscriptionService.findAll().size();
            return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalAdmins", totalAdmins,
                "totalSubscriptions", totalSubscriptions
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving stats"));
        }
    }

    // Freebie management endpoints
    @GetMapping("/settings/freebies")
    public String freebiesSettings(Model model) {
        // Get current default free chat limit (could be from config or database)
        int currentFreeChatLimit = 2; // Default value, in production this could be configurable
        model.addAttribute("currentFreeChatLimit", currentFreeChatLimit);
        return "admin/freebies-settings";
    }

    @PostMapping("/api/settings/freebies")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateFreebies(@RequestBody Map<String, Integer> payload) {
        try {
            Integer newFreeChatLimit = payload.get("freeChatLimit");
            if (newFreeChatLimit == null || newFreeChatLimit < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid free chat limit value"
                ));
            }

            // In a real application, this would be saved to a configuration table
            // For now, we'll just return success
            // You could update all existing users or set a global default

            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Free chat limit updated successfully",
                "newLimit", newFreeChatLimit
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while updating freebies"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Note: reset-freebies endpoint removed as freeChatLimit is no longer in User entity

    // Admin chat endpoints
    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendMessageToUser(@RequestBody Map<String, Object> payload) {
        try {
            Integer receiverId = (Integer) payload.get("receiverId");
            String content = (String) payload.get("content");

            User receiver = userService.findById(receiverId)
                    .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

            // Assume admin is the first admin user or create a dummy admin user
            User admin = userService.findAll(null).stream()
                    .filter(u -> u.getRole() == Role.ADMIN)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No admin user found"));

            Message message = new Message();
            message.setSender(admin);
            message.setReceiver(receiver);
            message.setContent(content);

            Message savedMessage = chatService.sendMessageAsAdmin(message);
            return ResponseEntity.ok(savedMessage);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while sending the message"));
        }
    }

    @GetMapping("/api/chat/messages/{userId}")
    @ResponseBody
    public ResponseEntity<?> getMessagesForUser(@PathVariable Integer userId) {
        try {
            User user = userService.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            List<Message> messages = chatService.getMessagesForUser(user);
            return ResponseEntity.ok(messages);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while retrieving messages"));
        }
    }

    // Admin plan management endpoints
    @GetMapping("/plans")
    public String getAllPlans(Model model) {
        model.addAttribute("plans", planService.getAllPlans());
        return "admin/plans";
    }

    @GetMapping("/plans/new")
    public String newPlanForm(Model model) {
        model.addAttribute("plan", new Plan());
        return "admin/plan-form";
    }

    @PostMapping("/plans")
    public String createPlan(@ModelAttribute Plan plan) {
        try {
            planService.savePlan(plan);
            return "redirect:/admin/plans";
        } catch (Exception e) {
            return "redirect:/admin/plans?error=" + e.getMessage();
        }
    }

    @GetMapping("/plans/{id}/edit")
    public String editPlanForm(@PathVariable Integer id, Model model) {
        planService.getPlanById(id).ifPresent(plan -> model.addAttribute("plan", plan));
        return "admin/plan-form";
    }

    @PostMapping("/plans/{id}")
    public String updatePlan(@PathVariable Integer id, @ModelAttribute Plan plan) {
        try {
            plan.setId(id);
            planService.savePlan(plan);
            return "redirect:/admin/plans";
        } catch (Exception e) {
            return "redirect:/admin/plans?error=" + e.getMessage();
        }
    }

    @PostMapping("/plans/{id}/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePlan(@PathVariable Integer id) {
        try {
            planService.deletePlan(id);
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Plan deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while deleting the plan"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }



    @PostMapping("/api/plans")
    @ResponseBody
    public ResponseEntity<?> createPlanApi(@RequestBody Plan plan) {
        try {
            Plan savedPlan = planService.savePlan(plan);
            return ResponseEntity.ok(savedPlan);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while creating the plan"));
        }
    }

    @PutMapping("/api/plans/{id}")
    @ResponseBody
    public ResponseEntity<?> updatePlanApi(@PathVariable Integer id, @RequestBody Plan plan) {
        try {
            plan.setId(id);
            Plan updatedPlan = planService.savePlan(plan);
            return ResponseEntity.ok(updatedPlan);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while updating the plan"));
        }
    }

    @DeleteMapping("/api/plans/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePlanApi(@PathVariable Integer id) {
        try {
            planService.deletePlan(id);
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Plan deleted successfully"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while deleting the plan"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    // Admin subscription management endpoints
    @PostMapping("/api/users/{id}/give-subscription")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> giveSubscription(@PathVariable Integer id, @RequestBody AdminGiveSubscriptionDto dto) {
        try {
            Subscription subscription = subscriptionService.giveSubscription(id, dto.getPlanId(), dto.getDurationMonths());
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Subscription given successfully",
                "subscription", subscription
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while giving subscription"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/api/users/{id}/revoke-subscription")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> revokeSubscription(@PathVariable Integer id) {
        try {
            subscriptionService.revokeSubscription(id);
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "Subscription revoked successfully"
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", e.getMessage()
            );
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            Map<String, Object> response = Map.of(
                "success", false,
                "message", "An error occurred while revoking subscription"
            );
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
