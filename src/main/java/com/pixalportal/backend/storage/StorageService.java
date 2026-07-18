package com.pixalportal.backend.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
public class StorageService {
    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);
    private final S3Client s3Client;
    private final S3Presigner s3Presigner; // Add this

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public StorageService(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Creates a virtual folder in S3 for a specific event.
     */
    public void createEventFolder(UUID eventId) {
        // The key must end with a trailing slash to be treated as a folder
        String folderKey = "events/" + eventId.toString() + "/";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(folderKey)
                .build();

        // Uploading an empty payload creates the "folder"
        s3Client.putObject(putObjectRequest, RequestBody.empty());
        
        System.out.println("Successfully created S3 folder: " + folderKey);
    }

    public void deleteEventFolder(UUID eventId) {
        String folderKey = "events/" + eventId.toString() + "/";
        
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(folderKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        logger.info("Successfully deleted S3 folder: {}", folderKey);
    }

    /**
     * Generates a pre-signed URL for guest uploads.
     */
    public String generatePresignedUploadUrl(UUID eventId, String fileName) {
        String objectKey = "events/" + eventId.toString() + "/" + fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        // Create the pre-sign request (URL valid for 15 minutes)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(putObjectRequest)
                .build();

        String url = s3Presigner.presignPutObject(presignRequest).url().toString();
        
        logger.info("Generated pre-signed URL for event: {}", eventId);
        return url;
    }

    
}