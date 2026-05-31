package com.jobify.api.service;

import com.google.cloud.storage.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class GcsService {

    @Value("${gcs.bucket.name}")
    private String bucketName;

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

    public byte[] downloadBytes(String gcsPath) {
        try {
            return storage.readAllBytes(BlobId.of(bucketName, gcsPath));
        } catch (Exception e) {
            log.error("Failed to download GCS file {}: {}", gcsPath, e.getMessage());
            throw new RuntimeException("GCS download failed: " + e.getMessage(), e);
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
