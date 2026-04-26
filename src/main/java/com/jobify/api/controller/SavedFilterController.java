package com.jobify.api.controller;

import com.jobify.api.dto.SavedFilterRequest;
import com.jobify.api.dto.SavedFilterResponse;
import com.jobify.api.service.SavedFilterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/filters")
@Tag(name = "Saved Filters", description = "Manage user-saved job search filter presets")
@RequiredArgsConstructor
public class SavedFilterController {

    private final SavedFilterService savedFilterService;

    @Operation(summary = "List saved filters", description = "Returns all saved filter presets for the authenticated user")
    @GetMapping
    public ResponseEntity<List<SavedFilterResponse>> getFilters(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(savedFilterService.getFilters(principal.getName()));
    }

    @Operation(summary = "Create saved filter", description = "Creates a new saved filter preset (limit enforced)")
    @PostMapping
    public ResponseEntity<?> createFilter(Principal principal, @RequestBody SavedFilterRequest request) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            SavedFilterResponse response = savedFilterService.createFilter(principal.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "Update saved filter", description = "Updates name and/or filters of an existing preset")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFilter(Principal principal,
                                          @PathVariable Long id,
                                          @RequestBody SavedFilterRequest request) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(savedFilterService.updateFilter(principal.getName(), id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @Operation(summary = "Delete saved filter", description = "Deletes a saved filter preset by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFilter(Principal principal, @PathVariable Long id) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            savedFilterService.deleteFilter(principal.getName(), id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
