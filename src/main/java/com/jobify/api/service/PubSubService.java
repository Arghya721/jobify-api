package com.jobify.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class PubSubService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${pubsub.project-id}")
    private String projectId;

    @Value("${pubsub.topic.resume-analysis}")
    private String resumeAnalysisTopic;

    public void publishResumeAnalysis(Long uploadId, String gcsPath, Long userId) {
        try {
            TopicName topicName = TopicName.of(projectId, resumeAnalysisTopic);
            Publisher publisher = Publisher.newBuilder(topicName).build();

            String payload = MAPPER.writeValueAsString(Map.of(
                    "upload_id", uploadId,
                    "gcs_path", gcsPath,
                    "user_id", userId
            ));

            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFromUtf8(payload))
                    .build();

            publisher.publish(message).get();
            publisher.shutdown();

            log.info("Published resume analysis message for upload_id={}", uploadId);
        } catch (Exception e) {
            log.error("Failed to publish Pub/Sub message for upload_id={}: {}", uploadId, e.getMessage());
            throw new RuntimeException("Pub/Sub publish failed: " + e.getMessage(), e);
        }
    }
}
