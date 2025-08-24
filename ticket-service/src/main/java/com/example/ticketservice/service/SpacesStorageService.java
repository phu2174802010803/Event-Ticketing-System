package com.example.ticketservice.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Service
public class SpacesStorageService {

    private static final Logger logger = LoggerFactory.getLogger(SpacesStorageService.class);

    @Autowired
    private S3Client s3Client;

    private final String bucketName = "ticket-qr-codes"; // Thay bằng tên Space
    private final String cdnEndpoint = "https://ticket-qr-codes.sgp1.cdn.digitaloceanspaces.com";

    public String uploadQRCode(byte[] qrCodeBytes, String fileName) throws IOException {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType("image/png")
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(qrCodeBytes));

            // Return CDN URL for better performance
            return cdnEndpoint + "/" + fileName;
        } catch (Exception e) {
            logger.error("Error uploading QR code to DigitalOcean Spaces: {}", e.getMessage(), e);
            throw new IOException("Failed to upload QR code: " + e.getMessage(), e);
        }
    }
}