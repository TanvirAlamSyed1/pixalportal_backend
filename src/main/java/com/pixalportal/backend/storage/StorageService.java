package com.pixalportal.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Service
public class StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
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
}