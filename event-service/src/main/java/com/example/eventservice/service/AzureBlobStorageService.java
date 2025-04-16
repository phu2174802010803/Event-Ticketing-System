package com.example.eventservice.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class AzureBlobStorageService {

    @Autowired
    private BlobServiceClient blobServiceClient;

    private final String containerName = "event-images";

    public String uploadImage(MultipartFile file) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // Thiết lập Content-Type dựa trên loại file
        String contentType = file.getContentType();
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType(contentType);

        // Tải file lên và áp dụng headers
        blobClient.upload(file.getInputStream(), file.getSize(), true);
        blobClient.setHttpHeaders(headers);

        return blobClient.getBlobUrl();
    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        try {
            String blobName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(blobName);
            blobClient.deleteIfExists();
        } catch (Exception e) {
            System.err.println("Error deleting image: " + e.getMessage());
        }
    }
}