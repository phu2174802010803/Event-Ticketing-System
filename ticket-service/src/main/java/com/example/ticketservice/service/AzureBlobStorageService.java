package com.example.ticketservice.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class AzureBlobStorageService {

    @Autowired
    private BlobServiceClient blobServiceClient;

    private final String containerName = "ticket-qr-codes";

    public String uploadQRCode(byte[] qrCodeBytes, String fileName) throws IOException {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // Upload the file
        blobClient.upload(new ByteArrayInputStream(qrCodeBytes), qrCodeBytes.length, true);

        // Set the Content-Type to image/png
        BlobHttpHeaders headers = new BlobHttpHeaders().setContentType("image/png");
        blobClient.setHttpHeaders(headers);

        return blobClient.getBlobUrl();
    }
}