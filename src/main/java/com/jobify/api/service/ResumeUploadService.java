package com.jobify.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobify.api.dto.ResumeSubmitResponse;
import com.jobify.api.dto.ResumeUploadResponse;
import com.jobify.api.model.ResumeUpload;
import com.jobify.api.model.User;
import com.jobify.api.repository.ResumeUploadRepository;
import com.jobify.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ResumeUploadRepository resumeUploadRepository;
    private final UserRepository userRepository;
    private final GcsService gcsService;
    private final PubSubService pubSubService;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.max-resume-uploads:3}")
    private int maxResumeUploads;

    // ─────────────────── Submit ───────────────────

    @Transactional
    public ResumeSubmitResponse submit(MultipartFile file, String email) {
        validateFile(file);

        User user = resolveUser(email);
        long total = resumeUploadRepository.countByUserId(user.getId());
        if (total >= maxResumeUploads) {
            throw new RuntimeException(
                    "Upload limit reached (" + maxResumeUploads + "). Delete an existing resume to free a slot."
            );
        }

        long active = resumeUploadRepository.countActiveByUserId(user.getId());
        if (active > 0) {
            throw new IllegalStateException(
                    "Another resume is currently being processed. Wait for it to complete before uploading a new one."
            );
        }

        String uuid = UUID.randomUUID().toString();
        String safeFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "resume.pdf";
        String gcsPath = "resumes/" + user.getId() + "/" + uuid + "_" + safeFileName;

        String uploadedPath = gcsService.uploadFile(file, gcsPath);

        ResumeUpload upload = ResumeUpload.builder()
                .user(user)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf")
                .gcsPath(uploadedPath)
                .status("pending")
                .build();
        upload = resumeUploadRepository.save(upload);

        pubSubService.publishResumeAnalysis(upload.getId(), uploadedPath, user.getId());

        return ResumeSubmitResponse.builder()
                .uploadId(upload.getId())
                .status("pending")
                .build();
    }

    // ─────────────────── List ───────────────────

    @Transactional(readOnly = true)
    public List<ResumeUploadResponse> getUploads(String email) {
        User user = resolveUser(email);
        return resumeUploadRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────── Delete ───────────────────

    @Transactional
    public void deleteUpload(Long uploadId, String email) {
        User user = resolveUser(email);
        ResumeUpload upload = resumeUploadRepository
                .findByIdAndUserId(uploadId, user.getId())
                .orElseThrow(() -> new RuntimeException("Resume upload not found or access denied."));

        if ("pending".equals(upload.getStatus()) || "processing".equals(upload.getStatus())) {
            throw new IllegalStateException("Cannot delete a resume that is currently being processed.");
        }

        gcsService.deleteFile(upload.getGcsPath());
        resumeUploadRepository.delete(upload);

        // Clean up Redis result key if present
        stringRedisTemplate.delete("resume:" + uploadId + ":result");
    }

    // ─────────────────── Download bytes ───────────────────

    @Transactional(readOnly = true)
    public byte[] downloadFile(Long uploadId, String email) {
        User user = resolveUser(email);
        ResumeUpload upload = resumeUploadRepository
                .findByIdAndUserId(uploadId, user.getId())
                .orElseThrow(() -> new RuntimeException("Resume upload not found or access denied."));

        return gcsService.downloadBytes(upload.getGcsPath());
    }

    // ─────────────────── Redis result (for SSE reconnect) ───────────────────

    public String getCachedResult(Long uploadId) {
        return stringRedisTemplate.opsForValue().get("resume:" + uploadId + ":result");
    }

    @Transactional(readOnly = true)
    public ResumeUpload getUploadEntity(Long uploadId, String email) {
        User user = resolveUser(email);
        return resumeUploadRepository
                .findByIdAndUserId(uploadId, user.getId())
                .orElseThrow(() -> new RuntimeException("Resume upload not found or access denied."));
    }

    // ─────────────────── Helpers ───────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty.");
        }
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!name.endsWith(".pdf") && !name.endsWith(".txt")) {
            throw new IllegalArgumentException("Only PDF and TXT files are accepted.");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must not exceed 10 MB.");
        }
    }

    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private ResumeUploadResponse toResponse(ResumeUpload u) {
        return ResumeUploadResponse.builder()
                .id(u.getId())
                .fileName(u.getFileName())
                .status(u.getStatus())
                .jobsQuery(u.getJobsQuery())
                .error(u.getError())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }

    public String buildCompletedPayload(ResumeUpload upload) {
        try {
            Map<String, Object> payload = Map.of(
                    "status", upload.getStatus(),
                    "jobs_query", upload.getJobsQuery() != null ? upload.getJobsQuery() : Map.of()
            );
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{\"status\":\"" + upload.getStatus() + "\"}";
        }
    }
}
