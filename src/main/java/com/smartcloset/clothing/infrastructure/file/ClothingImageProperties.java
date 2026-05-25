package com.smartcloset.clothing.infrastructure.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "smartcloset.clothing.image")
public class ClothingImageProperties {

    public static final String DEFAULT_STORAGE_DIR = "./uploads/clothing-images";
    public static final long DEFAULT_MAX_SIZE_BYTES = 5_242_880L;

    private String storageDir = DEFAULT_STORAGE_DIR;

    private long maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;

    public String storageDir() {
        return storageDir;
    }

    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    public String getStorageDir() {
        return storageDir;
    }

    public void setStorageDir(String storageDir) {
        this.storageDir = storageDir == null || storageDir.isBlank() ? DEFAULT_STORAGE_DIR : storageDir;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }
}
