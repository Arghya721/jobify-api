package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationSettingsResponse {

    @JsonProperty("telegram_chat_id")
    private String telegramChatId;

    @JsonProperty("discord_user_id")
    private String discordUserId;
}
