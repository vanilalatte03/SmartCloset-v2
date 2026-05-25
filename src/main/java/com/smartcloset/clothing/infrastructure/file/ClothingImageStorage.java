package com.smartcloset.clothing.infrastructure.file;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

public interface ClothingImageStorage {

    StoredClothingImage store(MultipartFile image, String extension);

    Optional<byte[]> read(String storedFilename);

    void delete(String storedFilename);
}
