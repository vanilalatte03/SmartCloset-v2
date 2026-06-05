package com.smartcloset.clothing.infrastructure.file;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 옷 이미지를 로컬 파일 시스템에 저장하는 현재 MVP용 storage adapter다.
 *
 * <p>저장 파일명은 서버가 만든 UUID만 허용하고, 읽기/삭제 시에도 storage root 밖으로
 * 나가지 못하게 path traversal을 방어한다.</p>
 */
@Component
public class FileSystemClothingImageStorage implements ClothingImageStorage {

    private final Path storageRoot;

    public FileSystemClothingImageStorage(ClothingImageProperties properties) {
        this.storageRoot = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    }

    /**
     * multipart 이미지 stream을 서버 생성 파일명으로 저장한다.
     */
    @Override
    public StoredClothingImage store(MultipartFile image, String extension) {
        try {
            try (InputStream inputStream = image.getInputStream()) {
                return store(inputStream, extension);
            }
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 메모리에 있는 이미지 bytes를 서버 생성 파일명으로 저장한다.
     */
    @Override
    public StoredClothingImage store(byte[] bytes, String extension) {
        try {
            return store(new java.io.ByteArrayInputStream(bytes), extension);
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DB에 저장된 파일명으로 이미지 bytes를 읽고 파일이 없으면 빈 Optional을 반환한다.
     */
    @Override
    public Optional<byte[]> read(String storedFilename) {
        Path source = resolve(storedFilename);
        if (!Files.isRegularFile(source)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(source));
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DB에 저장된 파일명의 이미지를 멱등하게 삭제한다.
     */
    @Override
    public void delete(String storedFilename) {
        if (storedFilename == null || storedFilename.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedFilename));
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * DB에 저장된 filename을 실제 Path로 바꾸되, 하위 디렉터리나 절대 경로가 들어오면 거부한다.
     */
    private Path resolve(String storedFilename) {
        try {
            Path filename = Path.of(storedFilename).getFileName();
            if (filename == null || !filename.toString().equals(storedFilename)) {
                throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            Path resolved = storageRoot.resolve(filename).normalize();
            if (!resolved.startsWith(storageRoot)) {
                throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
            return resolved;
        } catch (InvalidPathException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private StoredClothingImage store(InputStream inputStream, String extension) throws IOException {
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path target = resolve(storedFilename);
        Files.createDirectories(storageRoot);
        Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        return new StoredClothingImage(storedFilename, target);
    }
}
