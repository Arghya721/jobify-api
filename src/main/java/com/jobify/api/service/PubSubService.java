package com.jobify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PubSubService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PUBSUB_SCOPE = "https://www.googleapis.com/auth/pubsub";

    private final RestClient restClient = RestClient.create();

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.topic.resume-analysis}")
    private String resumeAnalysisTopic;

    public void publishResumeAnalysis(Long uploadId, String gcsPath, Long userId) {
        try {
            GoogleCredentials credentials = GoogleCredentials
                    .getApplicationDefault()
                    .createScoped(Collections.singletonList(PUBSUB_SCOPE));
            credentials.refreshIfExpired();
            String token = credentials.getAccessToken().getTokenValue();

            String data = Base64.getEncoder().encodeToString(
                    MAPPER.writeValueAsString(Map.of(
                            "upload_id", uploadId,
                            "gcs_path", gcsPath,
                            "user_id", userId
                    )).getBytes(StandardCharsets.UTF_8)
            );

            Map<String, Object> body = Map.of(
                    "messages", List.of(Map.of("data", data))
            );

            String url = "https://pubsub.googleapis.com/v1/projects/"
                    + projectId + "/topics/" + resumeAnalysisTopic + ":publish";

            restClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Published resume analysis message for upload_id={}", uploadId);
        } catch (Exception e) {
            log.error("Failed to publish Pub/Sub message for upload_id={}: {}", uploadId, e.getMessage());
            throw new RuntimeException("Pub/Sub publish failed: " + e.getMessage(), e);
        }
    }
}
