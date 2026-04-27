package com.jobify.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.util.HexFormat;
import java.util.Map;

/**
 * Sends Discord DMs via the Bot API and verifies incoming interaction signatures.
 *
 * Discord requires Ed25519 signature verification on every interaction request.
 * Requests that fail verification MUST be rejected with HTTP 401, or Discord
 * will remove your interaction endpoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordBotService {

    @Value("${bot.discord.token}")
    private String botToken;

    @Value("${bot.discord.public-key}")
    private String publicKeyHex;

    private final RestClient restClient = RestClient.create();

    // ─── Signature Verification ────────────────────────────────────────────────

    /**
     * Verifies the Ed25519 signature sent by Discord on every interaction.
     * Discord requires this to be validated before processing the payload.
     *
     * @param timestamp  Value of the X-Signature-Timestamp header
     * @param signature  Value of the X-Signature-Ed25519 header (hex)
     * @param rawBody    The raw request body bytes
     * @return true if the signature is valid
     */
    public boolean verifySignature(String timestamp, String signature, byte[] rawBody) {
        try {
            byte[] pubKeyBytes = HexFormat.of().parseHex(publicKeyHex);
            byte[] sigBytes = HexFormat.of().parseHex(signature);

            // Message = timestamp bytes + body bytes
            byte[] timestampBytes = timestamp.getBytes(StandardCharsets.UTF_8);
            byte[] message = new byte[timestampBytes.length + rawBody.length];
            System.arraycopy(timestampBytes, 0, message, 0, timestampBytes.length);
            System.arraycopy(rawBody, 0, message, timestampBytes.length, rawBody.length);

            // Standard ASN.1 prefix for Ed25519 public keys (SubjectPublicKeyInfo)
            byte[] ed25519Prefix = new byte[]{
                    0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x70, 0x03, 0x21, 0x00
            };
            
            // Prepend the prefix to the raw 32-byte public key
            byte[] x509Key = new byte[ed25519Prefix.length + pubKeyBytes.length];
            System.arraycopy(ed25519Prefix, 0, x509Key, 0, ed25519Prefix.length);
            System.arraycopy(pubKeyBytes, 0, x509Key, ed25519Prefix.length, pubKeyBytes.length);

            // Generate the public key using standard Java X.509 decoding
            java.security.spec.X509EncodedKeySpec keySpec = new java.security.spec.X509EncodedKeySpec(x509Key);
            java.security.KeyFactory kf = java.security.KeyFactory.getInstance("Ed25519");
            java.security.PublicKey publicKey = kf.generatePublic(keySpec);

            java.security.Signature verifier = java.security.Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(message);
            return verifier.verify(sigBytes);
        } catch (Exception e) {
            log.warn("Discord signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ─── Messaging ─────────────────────────────────────────────────────────────

    /**
     * Creates a DM channel with the given Discord user ID and sends a message.
     */
    public void sendDM(String discordUserId, String content) {
        try {
            // Step 1: Create DM channel
            Map<?, ?> channelResponse = restClient.post()
                    .uri("https://discord.com/api/v10/users/@me/channels")
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("recipient_id", discordUserId))
                    .retrieve()
                    .body(Map.class);

            if (channelResponse == null || !channelResponse.containsKey("id")) {
                log.error("Failed to create DM channel for discordUserId={}", discordUserId);
                return;
            }
            String channelId = (String) channelResponse.get("id");

            // Step 2: Send message to DM channel
            restClient.post()
                    .uri("https://discord.com/api/v10/channels/{channelId}/messages", channelId)
                    .header("Authorization", "Bot " + botToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("content", content))
                    .retrieve()
                    .toBodilessEntity();

        } catch (Exception e) {
            log.error("Failed to send Discord DM to userId={}: {}", discordUserId, e.getMessage());
        }
    }
}
