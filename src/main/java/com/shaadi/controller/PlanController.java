package com.shaadi.controller;

import com.shaadi.entity.Plan;
import com.shaadi.service.PlanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<?> getAllPlans() {
        try {
            List<Plan> plans = planService.getAllPlans();
            return ResponseEntity.ok(plans);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to retrieve plans"));
        }
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody Plan plan) {
        try {
            Plan savedPlan = planService.savePlan(plan);
            return ResponseEntity.ok(savedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Integer id, @RequestBody Plan plan) {
        Optional<Plan> existingPlan = planService.getPlanById(id);
        if (existingPlan.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Plan not found"));
        }
        try {
            plan.setId(id);
            Plan updatedPlan = planService.savePlan(plan);
            return ResponseEntity.ok(updatedPlan);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(@PathVariable Integer id) {
        Optional<Plan> existingPlan = planService.getPlanById(id);
        if (existingPlan.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Plan not found"));
        }
        try {
            planService.deletePlan(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to delete plan"));
        }
    }
}
