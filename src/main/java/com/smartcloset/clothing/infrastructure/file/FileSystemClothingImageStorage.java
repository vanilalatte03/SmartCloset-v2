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

@Component
public class FileSystemClothingImageStorage implements ClothingImageStorage {

    private final Path storageRoot;

    public FileSystemClothingImageStorage(ClothingImageProperties properties) {
        this.storageRoot = Path.of(properties.storageDir()).toAbsolutePath().normalize();
    }

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

    @Override
    public StoredClothingImage store(byte[] bytes, String extension) {
        try {
            return store(new java.io.ByteArrayInputStream(bytes), extension);
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

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
