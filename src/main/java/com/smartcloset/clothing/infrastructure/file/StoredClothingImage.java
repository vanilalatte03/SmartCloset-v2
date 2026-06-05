package com.smartcloset.clothing.infrastructure.file;

import java.nio.file.Path;

/**
 * 저장소에 기록된 이미지의 서버 생성 파일명과 실제 경로를 함께 담는 value object다.
 */
public record StoredClothingImage(String storedFilename, Path path) {
}
