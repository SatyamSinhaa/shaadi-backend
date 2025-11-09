package com.shaadi.controller;

import com.shaadi.entity.Plan;
import com.shaadi.service.PlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin
public class PlanController {
    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ResponseEntity<List<Plan>> getAllPublishedPlans() {
        List<Plan> plans = planService.getAllPublishedPlans();
        return ResponseEntity.ok(plans);
    }

    @PostMapping
    public ResponseEntity<Plan> createPlan(@RequestBody Plan plan) {
        Plan savedPlan = planService.savePlan(plan);
        return ResponseEntity.ok(savedPlan);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Plan> updatePlan(@PathVariable Integer id, @RequestBody Plan plan) {
        plan.setId(id);
        Plan updatedPlan = planService.savePlan(plan);
        return ResponseEntity.ok(updatedPlan);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Integer id) {
        planService.deletePlan(id);
        return ResponseEntity.noContent().build();
    }
}
