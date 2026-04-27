package com.jobify.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobify.api.service.DiscordBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles Discord Interaction webhooks (slash commands).
 *
 * Setup (one-time, in Discord Developer Portal):
 *   1. Go to your application → "Interactions Endpoint URL"
 *   2. Set it to: https://your-api.com/api/bot/discord/interactions
 *   3. Register the /id slash command (see DiscordCommandRegistrar below)
 *
 * Discord's Interaction Flow:
 *   1. User types /id in any channel where the bot has access
 *   2. Discord POST's to your interactions endpoint
 *   3. You verify the Ed25519 signature (REQUIRED)
 *   4. You respond within 3 seconds
 *
 * Response Types:
 *   Type 1 = PONG (for Discord's verification ping)
 *   Type 4 = CHANNEL_MESSAGE_WITH_SOURCE
 *   Flag 64 = EPHEMERAL (only visible to the invoking user)
 */
@Slf4j
@RestController
@RequestMapping("/api/bot/discord")
@RequiredArgsConstructor
public class DiscordInteractionsController {

    private final DiscordBotService discordBotService;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Discord Interactions endpoint.
     * We consume the raw body as bytes so we can verify the signature before parsing.
     */
    @PostMapping(value = "/interactions", consumes = "application/json")
    public ResponseEntity<?> handleInteraction(
            @RequestHeader("X-Signature-Ed25519") String signature,
            @RequestHeader("X-Signature-Timestamp") String timestamp,
            @RequestBody byte[] rawBody
    ) {
        // Step 1: Verify Ed25519 signature — Discord will deregister endpoints that skip this
        if (!discordBotService.verifySignature(timestamp, signature, rawBody)) {
            log.warn("Discord interaction: invalid signature");
            return ResponseEntity.status(401).body("Invalid request signature");
        }

        try {
            JsonNode body = OBJECT_MAPPER.readTree(rawBody);
            int type = body.path("type").asInt();

            // Step 2: Discord sends a PING (type=1) when verifying the endpoint URL
            if (type == 1) {
                log.info("Discord PING — responding with PONG");
                return ResponseEntity.ok(Map.of("type", 1));
            }

            // Step 3: Slash command (type=2)
            if (type == 2) {
                String commandName = body.path("data").path("name").asText("");
                // In a server: user is inside "member.user"; in a DM it's directly "user"
                String discordUserId = body.path("member").path("user").path("id").asText("");
                if (discordUserId.isEmpty()) {
                    discordUserId = body.path("user").path("id").asText("");
                }

                log.info("Discord slash /{} from userId={}", commandName, discordUserId);

                if ("id".equals(commandName) && !discordUserId.isEmpty()) {
                    String message = String.format(
                            "🤖 **Your Jobify User ID is:**\n```\n%s\n```\nPaste this into your Jobify notification settings to receive job alerts!",
                            discordUserId
                    );
                    // EPHEMERAL (flag=64) means only the requesting user can see the reply
                    return ResponseEntity.ok(Map.of(
                            "type", 4,
                            "data", Map.of("content", message, "flags", 64)
                    ));
                }
            }

            // Fallback: acknowledge unknown interaction types
            return ResponseEntity.ok(Map.of("type", 1));

        } catch (Exception e) {
            log.error("Discord interaction processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
