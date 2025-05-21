package com.example.eventservice.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

@Service
public class SpacesStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SpacesStorageService.class);

    @Autowired
    private S3Client s3Client;

    private final String bucketName = "event-images-system"; // Thay bằng tên Space
    private final String cdnEndpoint = "https://event-images-system.sgp1.cdn.digitaloceanspaces.com";

    public String uploadImage(MultipartFile file) throws IOException {
        return uploadFile(file);
    }

    public String uploadFile(MultipartFile file) throws IOException {
        try {
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Return CDN URL for better performance
            return cdnEndpoint + "/" + fileName;
        } catch (Exception e) {
            logger.error("Error uploading file to DigitalOcean Spaces: {}", e.getMessage(), e);
            throw new IOException("Failed to upload file: " + e.getMessage(), e);
        }
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        try {
            // Parse fileName from URL correctly
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (Exception e) {
            logger.error("Error deleting file from DigitalOcean Spaces: {}", e.getMessage(), e);
        }
    }
}