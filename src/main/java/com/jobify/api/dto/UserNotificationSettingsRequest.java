package com.jobify.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserNotificationSettingsRequest {

    @JsonProperty("telegram_chat_id")
    private String telegramChatId;

    @JsonProperty("discord_user_id")
    private String discordUserId;
}
