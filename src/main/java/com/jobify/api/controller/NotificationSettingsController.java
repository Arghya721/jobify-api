package com.jobify.api.controller;

import com.jobify.api.dto.UserNotificationSettingsRequest;
import com.jobify.api.dto.UserNotificationSettingsResponse;
import com.jobify.api.service.NotificationSettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications/settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @GetMapping
    public ResponseEntity<UserNotificationSettingsResponse> getSettings(@AuthenticationPrincipal String email) {
        return ResponseEntity.ok(settingsService.getSettings(email));
    }

    @PutMapping
    public ResponseEntity<UserNotificationSettingsResponse> updateSettings(
            @AuthenticationPrincipal String email,
            @RequestBody UserNotificationSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(email, request));
    }
}
