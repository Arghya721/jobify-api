package com.jobify.api.controller;

import com.jobify.api.dto.ResumeSubmitResponse;
import com.jobify.api.dto.ResumeUploadResponse;
import com.jobify.api.model.ResumeUpload;
import com.jobify.api.service.ResumeUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/resume")
@Tag(name = "Resume Analyser", description = "AI-powered resume analysis and job search filter generation")
@RequiredArgsConstructor
public class ResumeUploadController {

    private final ResumeUploadService resumeUploadService;
    private final RedisMessageListenerContainer redisMessageListenerContainer;

    // ─────────────────── Submit ───────────────────

    @Operation(summary = "Submit resume for analysis", description = "Uploads a resume and queues it for async AI analysis. Returns 202 immediately.")
    @PostMapping(value = "/analyse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyse(
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            ResumeSubmitResponse response = resumeUploadService.submit(file, principal.getName());
            return ResponseEntity.accepted().body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─────────────────── List uploads ───────────────────

    @Operation(summary = "List resume uploads", description = "Returns all resume uploads for the authenticated user, newest first.")
    @GetMapping("/uploads")
    public ResponseEntity<?> listUploads(Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<ResumeUploadResponse> uploads = resumeUploadService.getUploads(principal.getName());
        return ResponseEntity.ok(uploads);
    }

    // ─────────────────── Delete ───────────────────

    @Operation(summary = "Delete resume upload", description = "Deletes a resume upload and its GCS file. Returns 409 if upload is still processing.")
    @DeleteMapping("/uploads/{id}")
    public ResponseEntity<?> deleteUpload(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            resumeUploadService.deleteUpload(id, principal.getName());
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ─────────────────── Download signed URL ───────────────────

    @Operation(summary = "Get resume download URL", description = "Returns a short-lived signed GCS URL to view the uploaded resume file.")
    @GetMapping("/uploads/{id}/download")
    public ResponseEntity<?> downloadUrl(@PathVariable Long id, Principal principal) {
        if (principal == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            String url = resumeUploadService.getDownloadUrl(id, principal.getName());
            return ResponseEntity.ok(Map.of("url", url));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ─────────────────── SSE stream ───────────────────

    @Operation(summary = "Stream analysis status", description = "SSE endpoint. Returns immediately if result is cached or done; otherwise streams when the worker publishes the result.")
    @GetMapping(value = "/stream/{uploadId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long uploadId, Principal principal) {
        SseEmitter emitter = new SseEmitter(120_000L); // 2 min timeout

        if (principal == null) {
            sendAndComplete(emitter, "{\"status\":\"unauthorized\"}");
            return emitter;
        }

        // 1. Check Redis SETEX key — fastest path for reconnecting users
        String cached = resumeUploadService.getCachedResult(uploadId);
        if (cached != null) {
            sendAndComplete(emitter, cached);
            return emitter;
        }

        // 2. Check DB — handles case where Redis TTL expired but processing is done
        ResumeUpload upload;
        try {
            upload = resumeUploadService.getUploadEntity(uploadId, principal.getName());
        } catch (RuntimeException e) {
            sendAndComplete(emitter, "{\"status\":\"idle\"}");
            return emitter;
        }

        if ("completed".equals(upload.getStatus()) || "failed".equals(upload.getStatus())) {
            sendAndComplete(emitter, resumeUploadService.buildCompletedPayload(upload));
            return emitter;
        }

        // 3. Still pending/processing — subscribe to Redis PUBLISH channel
        String channel = "resume:" + uploadId;
        ChannelTopic topic = new ChannelTopic(channel);

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    String payload = new String(message.getBody());
                    emitter.send(SseEmitter.event().name("result").data(payload));
                    emitter.complete();
                } catch (IOException e) {
                    emitter.completeWithError(e);
                } finally {
                    redisMessageListenerContainer.removeMessageListener(this);
                }
            }
        };

        redisMessageListenerContainer.addMessageListener(listener, topic);

        emitter.onCompletion(() -> redisMessageListenerContainer.removeMessageListener(listener));
        emitter.onTimeout(() -> redisMessageListenerContainer.removeMessageListener(listener));
        emitter.onError((e) -> redisMessageListenerContainer.removeMessageListener(listener));

        return emitter;
    }

    // ─────────────────── Helper ───────────────────

    private void sendAndComplete(SseEmitter emitter, String payload) {
        try {
            emitter.send(SseEmitter.event().name("result").data(payload));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
