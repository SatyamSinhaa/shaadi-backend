package com.shaadi.controller;

import com.shaadi.entity.Plan;
import com.shaadi.service.PlanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanControllerTest {

    @Mock
    private PlanService planService;

    @InjectMocks
    private PlanController planController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllPublishedPlans_Success() {
        List<Plan> plans = Arrays.asList(new Plan(), new Plan());
        when(planService.getAllPublishedPlans()).thenReturn(plans);

        ResponseEntity<?> response = planController.getAllPublishedPlans();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(plans, response.getBody());
    }

    @Test
    void testGetAllPublishedPlans_Exception() {
        when(planService.getAllPublishedPlans()).thenThrow(new RuntimeException("Database error"));

        ResponseEntity<?> response = planController.getAllPublishedPlans();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Failed to retrieve plans", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testCreatePlan_Success() {
        Plan plan = new Plan();
        Plan savedPlan = new Plan();
        when(planService.savePlan(plan)).thenReturn(savedPlan);

        ResponseEntity<?> response = planController.createPlan(plan);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(savedPlan, response.getBody());
    }

    @Test
    void testCreatePlan_IllegalArgumentException() {
        Plan plan = new Plan();
        when(planService.savePlan(plan)).thenThrow(new IllegalArgumentException("Invalid plan data"));

        ResponseEntity<?> response = planController.createPlan(plan);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Invalid plan data", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testUpdatePlan_Success() {
        Integer id = 1;
        Plan plan = new Plan();
        Plan existingPlan = new Plan();
        Plan updatedPlan = new Plan();
        when(planService.getPlanById(id)).thenReturn(Optional.of(existingPlan));
        when(planService.savePlan(plan)).thenReturn(updatedPlan);

        ResponseEntity<?> response = planController.updatePlan(id, plan);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(updatedPlan, response.getBody());
    }

    @Test
    void testUpdatePlan_NotFound() {
        Integer id = 1;
        Plan plan = new Plan();
        when(planService.getPlanById(id)).thenReturn(Optional.empty());

        ResponseEntity<?> response = planController.updatePlan(id, plan);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Plan not found", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testUpdatePlan_IllegalArgumentException() {
        Integer id = 1;
        Plan plan = new Plan();
        Plan existingPlan = new Plan();
        when(planService.getPlanById(id)).thenReturn(Optional.of(existingPlan));
        when(planService.savePlan(plan)).thenThrow(new IllegalArgumentException("Invalid update"));

        ResponseEntity<?> response = planController.updatePlan(id, plan);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Invalid update", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testDeletePlan_Success() {
        Integer id = 1;
        Plan existingPlan = new Plan();
        when(planService.getPlanById(id)).thenReturn(Optional.of(existingPlan));

        ResponseEntity<?> response = planController.deletePlan(id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(planService, times(1)).deletePlan(id);
    }

    @Test
    void testDeletePlan_NotFound() {
        Integer id = 1;
        when(planService.getPlanById(id)).thenReturn(Optional.empty());

        ResponseEntity<?> response = planController.deletePlan(id);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Plan not found", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void testDeletePlan_Exception() {
        Integer id = 1;
        Plan existingPlan = new Plan();
        when(planService.getPlanById(id)).thenReturn(Optional.of(existingPlan));
        doThrow(new RuntimeException("Delete failed")).when(planService).deletePlan(id);

        ResponseEntity<?> response = planController.deletePlan(id);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(response.getBody() instanceof java.util.Map);
        assertEquals("Failed to delete plan", ((java.util.Map<?, ?>) response.getBody()).get("error"));
    }
}
