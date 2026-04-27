package com.jobify.api.service;

import com.jobify.api.dto.UserNotificationSettingsRequest;
import com.jobify.api.dto.UserNotificationSettingsResponse;
import com.jobify.api.model.User;
import com.jobify.api.model.UserNotificationSettings;
import com.jobify.api.repository.UserNotificationSettingsRepository;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final UserNotificationSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserNotificationSettingsResponse getSettings(String email) {
        User user = resolveUser(email);
        return settingsRepository.findById(user.getId())
                .map(this::toResponse)
                .orElse(new UserNotificationSettingsResponse(null, null));
    }

    @Transactional
    public UserNotificationSettingsResponse updateSettings(String email, UserNotificationSettingsRequest request) {
        User user = resolveUser(email);
        
        UserNotificationSettings settings = settingsRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserNotificationSettings newSettings = new UserNotificationSettings();
                    newSettings.setUser(user);
                    return newSettings;
                });
                
        settings.setTelegramChatId(request.getTelegramChatId());
        settings.setDiscordUserId(request.getDiscordUserId());
        
        return toResponse(settingsRepository.save(settings));
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private UserNotificationSettingsResponse toResponse(UserNotificationSettings entity) {
        return UserNotificationSettingsResponse.builder()
                .telegramChatId(entity.getTelegramChatId())
                .discordUserId(entity.getDiscordUserId())
                .build();
    }
}
