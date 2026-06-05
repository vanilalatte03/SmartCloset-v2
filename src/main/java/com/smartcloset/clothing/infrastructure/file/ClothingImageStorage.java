package com.smartcloset.clothing.infrastructure.file;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 옷 이미지 bytes 저장소를 application service에서 분리하는 adapter boundary다.
 *
 * <p>현재 구현은 로컬 파일 시스템이지만, 후속 storage adapter도 이 계약을 유지해야 한다.</p>
 */
public interface ClothingImageStorage {

    /**
     * multipart 이미지 stream을 저장하고 서버 생성 저장 파일명을 반환한다.
     */
    StoredClothingImage store(MultipartFile image, String extension);

    /**
     * seed/test asset처럼 이미 메모리에 있는 이미지 bytes를 저장한다.
     */
    StoredClothingImage store(byte[] bytes, String extension);

    /**
     * 저장 파일명을 기준으로 이미지 bytes를 읽고 없으면 빈 Optional을 반환한다.
     */
    Optional<byte[]> read(String storedFilename);

    /**
     * 저장 파일명이 비어 있거나 파일이 없어도 성공하는 멱등 삭제를 수행한다.
     */
    void delete(String storedFilename);
}
