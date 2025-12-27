package com.example.bookshelf.service;

import com.google.cloud.storage.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CloudStorageService {

    private final Storage storage;
    
    @Value("${spring.cloud.gcp.storage.bucket-name}")
    private String bucketName;

    public CloudStorageService() {
        this.storage = StorageOptions.getDefaultInstance().getService();
    }

    /**
     * Uploads an image to Google Cloud Storage
     * @param file The multipart file to upload
     * @return The public URL of the uploaded image
     * @throws IOException if upload fails
     */
    public String uploadImage(MultipartFile file) throws IOException {
        validateImageFile(file);
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = "book-covers/" + UUID.randomUUID().toString() + fileExtension;
        
        // Create blob info with metadata
        BlobId blobId = BlobId.of(bucketName, uniqueFilename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();
        
        // Upload file to GCS
        storage.create(blobInfo, file.getBytes());
        
        // Return the public URL
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, uniqueFilename);
    }

    /**
     * Deletes an image from Google Cloud Storage
     * @param imageUrl The full URL or blob name of the image to delete
     */
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        try {
            // Extract blob name from URL
            String blobName = extractBlobNameFromUrl(imageUrl);
            BlobId blobId = BlobId.of(bucketName, blobName);
            storage.delete(blobId);
        } catch (Exception e) {
            // Log error but don't throw exception
            System.err.println("Failed to delete image from GCS: " + imageUrl);
            e.printStackTrace();
        }
    }

    /**
     * Generates a signed URL for temporary access to a private image
     * @param blobName The blob name in GCS
     * @param duration The duration for which the URL is valid
     * @param timeUnit The time unit for the duration
     * @return The signed URL
     */
    public String generateSignedUrl(String blobName, long duration, TimeUnit timeUnit) {
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, blobName).build();
        return storage.signUrl(blobInfo, duration, timeUnit).toString();
    }

    /**
     * Validates the image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        // Check file size (10MB max)
        long maxSize = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum limit of 10MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Only image files are allowed");
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!extension.matches("jpg|jpeg|png|gif|bmp|webp")) {
                throw new IOException("Invalid image file extension. Allowed: jpg, jpeg, png, gif, bmp, webp");
            }
        }
    }

    /**
     * Extracts file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        return ".jpg";
    }

    /**
     * Extracts blob name from a GCS URL
     */
    private String extractBlobNameFromUrl(String url) {
        // Handle different URL formats:
        // https://storage.googleapis.com/bucket-name/blob-name
        // gs://bucket-name/blob-name
        if (url.startsWith("https://storage.googleapis.com/")) {
            String withoutProtocol = url.substring("https://storage.googleapis.com/".length());
            int firstSlash = withoutProtocol.indexOf('/');
            if (firstSlash != -1) {
                return withoutProtocol.substring(firstSlash + 1);
            }
        } else if (url.startsWith("gs://")) {
            String withoutProtocol = url.substring("gs://".length());
            int firstSlash = withoutProtocol.indexOf('/');
            if (firstSlash != -1) {
                return withoutProtocol.substring(firstSlash + 1);
            }
        }
        
        // If it's already just the blob name
        return url;
    }
}
