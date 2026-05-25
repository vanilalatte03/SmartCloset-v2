package com.smartcloset.clothing.infrastructure.file;

import java.nio.file.Path;

public record StoredClothingImage(String storedFilename, Path path) {
}
