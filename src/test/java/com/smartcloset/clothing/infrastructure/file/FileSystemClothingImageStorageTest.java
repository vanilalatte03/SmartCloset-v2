package com.smartcloset.clothing.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class FileSystemClothingImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void storesWithUuidFilenameAndCanReadAndDelete() throws Exception {
        FileSystemClothingImageStorage storage = storage();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "../../original.jpg",
                "image/jpeg",
                new byte[] {1, 2, 3}
        );

        StoredClothingImage stored = storage.store(file, "jpg");

        assertThat(stored.storedFilename()).endsWith(".jpg");
        assertThat(stored.storedFilename()).doesNotContain("original");
        assertThat(stored.path()).isEqualTo(tempDir.resolve(stored.storedFilename()).toAbsolutePath().normalize());
        assertThat(Files.readAllBytes(tempDir.resolve(stored.storedFilename()))).containsExactly(1, 2, 3);
        assertThat(storage.read(stored.storedFilename()))
                .hasValueSatisfying(bytes -> assertThat(bytes).containsExactly(1, 2, 3));

        storage.delete(stored.storedFilename());
        storage.delete(stored.storedFilename());

        assertThat(storage.read(stored.storedFilename())).isEmpty();
    }

    @Test
    void readReturnsEmptyWhenFileDoesNotExist() {
        FileSystemClothingImageStorage storage = storage();

        assertThat(storage.read("missing.jpg")).isEmpty();
    }

    private FileSystemClothingImageStorage storage() {
        ClothingImageProperties properties = new ClothingImageProperties();
        properties.setStorageDir(tempDir.toString());
        return new FileSystemClothingImageStorage(properties);
    }
}
