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
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    public Map<String, String> generatePresignedUploadUrl(UUID eventId, String fileName, String contentType) {
        // 1. Replicate the unique filename logic: Date.now() + UUID + fileName
        String uniqueName = System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-" + fileName;
        String objectKey = "events/" + eventId.toString() + "/" + uniqueName;

        // 2. Include the content type in the request
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        // 3. Create the pre-sign request (valid for 5 minutes / 300 seconds)
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        
        // Note: Replace "eu-west-2" with your actual AWS region if it differs
        String publicUrl = "https://" + bucketName + ".s3.eu-west-2.amazonaws.com/" + objectKey;

        // 4. Return both URLs in a Map so it serialises to JSON
        Map<String, String> response = new HashMap<>();
        response.put("uploadUrl", uploadUrl);
        response.put("publicUrl", publicUrl);
        
        return response;
    }

    public List<Map<String, String>> listEventImages(UUID eventId) {
        String prefix = "events/" + eventId.toString() + "/";

        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(prefix)
                .build();

        ListObjectsV2Response listResponse = s3Client.listObjectsV2(listRequest);
        List<Map<String, String>> signedUrls = new ArrayList<>();

        for (S3Object s3Object : listResponse.contents()) {
            // Filter out the "folder" itself (size 0)
            if (s3Object.size() != null && s3Object.size() > 0) {
                
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build();

                // Generate a URL valid for 1 hour (matching your Next.js logic)
                GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofHours(1))
                        .getObjectRequest(getObjectRequest)
                        .build();

                String url = s3Presigner.presignGetObject(presignRequest).url().toString();

                Map<String, String> imageInfo = new HashMap<>();
                imageInfo.put("key", s3Object.key());
                imageInfo.put("url", url);
                
                signedUrls.add(imageInfo);
            }
        }
        
        logger.info("Returned {} valid image URL(s) for event: {}", signedUrls.size(), eventId);
        return signedUrls;
    }

    public void deleteImage(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        logger.info("Successfully deleted S3 object with key: {}", key);
    }

    
}