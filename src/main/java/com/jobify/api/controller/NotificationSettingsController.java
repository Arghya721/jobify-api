package com.jobify.api.controller;

import com.jobify.api.dto.UserNotificationSettingsRequest;
import com.jobify.api.dto.UserNotificationSettingsResponse;
import com.jobify.api.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserNotificationSettingsResponse> getSettings(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(settingsService.getSettings(principal.getName()));
    }

    @PutMapping
    public ResponseEntity<UserNotificationSettingsResponse> updateSettings(
            Principal principal,
            @RequestBody UserNotificationSettingsRequest request) {
        if (principal == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(settingsService.updateSettings(principal.getName(), request));
    }
}
