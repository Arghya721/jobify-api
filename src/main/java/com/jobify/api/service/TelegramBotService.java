package com.jobify.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends messages via the Telegram Bot API.
 * Used by the webhook controller to reply to /id commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    @Value("${bot.telegram.token}")
    private String botToken;

    private final RestClient restClient = RestClient.create();

    /**
     * Sends a text message to a specific Telegram chat ID.
     */
    public void sendMessage(String chatId, String text) {
        String url = String.format("https://api.telegram.org/bot%s/sendMessage", botToken);
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "Markdown"
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId={}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Registers this server as the Telegram webhook.
     * Call this once after deploying (or expose as an admin endpoint).
     */
    public void registerWebhook(String webhookUrl) {
        String url = String.format("https://api.telegram.org/bot%s/setWebhook", botToken);
        try {
            restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("url", webhookUrl))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Telegram webhook registered: {}", webhookUrl);
        } catch (Exception e) {
            log.error("Failed to register Telegram webhook: {}", e.getMessage());
        }
    }
}
