package com.smartcloset.clothing.infrastructure.file;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 옷 이미지 저장 위치와 최대 업로드 크기를 환경별로 바인딩한다.
 *
 * <p>현재 구현은 로컬 파일 시스템 storage를 사용하며, DB에는 이미지 메타데이터만 저장한다.</p>
 */
@ConfigurationProperties(prefix = "smartcloset.clothing.image")
public class ClothingImageProperties {

    public static final String DEFAULT_STORAGE_DIR = "./uploads/clothing-images";
    public static final long DEFAULT_MAX_SIZE_BYTES = 5_242_880L;

    private String storageDir = DEFAULT_STORAGE_DIR;

    private long maxSizeBytes = DEFAULT_MAX_SIZE_BYTES;

    /**
     * adapter 코드에서 사용하는 이미지 storage directory 값을 반환한다.
     */
    public String storageDir() {
        return storageDir;
    }

    /**
     * 업로드 validation에서 사용하는 최대 이미지 크기(byte)를 반환한다.
     */
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
