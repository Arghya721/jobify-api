package com.jobify.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobify.api.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Handles incoming Telegram Bot webhook calls.
 *
 * Setup (one-time):
 *   POST https://api.telegram.org/bot{TOKEN}/setWebhook
 *   Body: {"url": "https://your-api.com/api/bot/telegram/webhook/{SECRET}"}
 *
 * The secret path token prevents unauthorized parties from posting fake updates.
 */
@Slf4j
@RestController
@RequestMapping("/api/bot/telegram")
@RequiredArgsConstructor
public class TelegramWebhookController {

    private final TelegramBotService telegramBotService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Value("${bot.telegram.webhook-secret}")
    private String webhookSecret;

    /**
     * Receives Telegram updates.
     * The path includes the webhook secret as a simple auth mechanism.
     */
    @PostMapping("/webhook/{secret}")
    public ResponseEntity<Void> handleUpdate(
            @PathVariable String secret,
            @RequestBody String rawUpdate
    ) {
        // Validate the secret
        if (!webhookSecret.equals(secret)) {
            log.warn("Telegram webhook: invalid secret received");
            return ResponseEntity.status(403).build();
        }

        try {
            JsonNode update = OBJECT_MAPPER.readTree(rawUpdate);
            JsonNode message = update.get("message");
            if (message == null || message.isNull()) {
                return ResponseEntity.ok().build(); // Ignore non-message updates
            }

            String text = message.path("text").asText("").trim();
            String chatId = String.valueOf(message.path("chat").path("id").asLong());

            log.info("Telegram update: chatId={}, text={}", chatId, text);

            if ("/id".equals(text) || "/id@JobifyNotifierBot".equals(text)) {
                String reply = String.format(
                        "🤖 *Your Jobify Chat ID is:*\n\n`%s`\n\nPaste this into your Jobify notification settings to receive job alerts!",
                        chatId
                );
                telegramBotService.sendMessage(chatId, reply);
            }

        } catch (Exception e) {
            log.error("Error processing Telegram update: {}", e.getMessage(), e);
        }

        // Always return 200 to Telegram so it doesn't retry
        return ResponseEntity.ok().build();
    }

    /**
     * Admin endpoint to register the webhook with Telegram.
     * Call this once after deploying: POST /api/bot/telegram/register-webhook?url=https://...
     */
    @PostMapping("/register-webhook")
    public ResponseEntity<String> registerWebhook(@RequestParam String url) {
        telegramBotService.registerWebhook(url + "/api/bot/telegram/webhook/" + webhookSecret);
        return ResponseEntity.ok("Webhook registered");
    }
}
