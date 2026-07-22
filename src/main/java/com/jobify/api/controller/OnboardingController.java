package com.jobify.api.controller;

import com.jobify.api.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping
    public ResponseEntity<Map<String, Boolean>> getStatus(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(Map.of("onboardingCompleted", onboardingService.isCompleted(principal.getName())));
    }

    @PostMapping("/complete")
    public ResponseEntity<Void> complete(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        onboardingService.markCompleted(principal.getName());
        return ResponseEntity.noContent().build();
    }
}
