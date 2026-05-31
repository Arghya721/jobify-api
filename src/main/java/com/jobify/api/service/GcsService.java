package com.jobify.api.service;

import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class GcsService {

    @Value("${gcs.bucket.name}")
    private String bucketName;

    @Value("${gcs.signed-url.duration-minutes:15}")
    private long signedUrlDurationMinutes;

    private final Storage storage = StorageOptions.getDefaultInstance().getService();

    public String uploadFile(MultipartFile file, String gcsPath) {
        try {
            BlobId blobId = BlobId.of(bucketName, gcsPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();
            storage.create(blobInfo, file.getBytes());
            log.info("Uploaded file to GCS: {}/{}", bucketName, gcsPath);
            return gcsPath;
        } catch (Exception e) {
            log.error("Failed to upload file to GCS path {}: {}", gcsPath, e.getMessage());
            throw new RuntimeException("GCS upload failed: " + e.getMessage(), e);
        }
    }

    public URL generateSignedUrl(String gcsPath) {
        try {
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, gcsPath)).build();
            return storage.signUrl(
                    blobInfo,
                    signedUrlDurationMinutes,
                    TimeUnit.MINUTES,
                    Storage.SignUrlOption.withV4Signature(),
                    Storage.SignUrlOption.signWith(
                            com.google.auth.oauth2.ComputeEngineCredentials.create()
                    )
            );
        } catch (Exception e) {
            log.error("Failed to generate signed URL for {}: {}", gcsPath, e.getMessage());
            throw new RuntimeException("Signed URL generation failed: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String gcsPath) {
        try {
            storage.delete(BlobId.of(bucketName, gcsPath));
            log.info("Deleted GCS file: {}/{}", bucketName, gcsPath);
        } catch (Exception e) {
            log.warn("Failed to delete GCS file {}: {}", gcsPath, e.getMessage());
        }
    }
}
