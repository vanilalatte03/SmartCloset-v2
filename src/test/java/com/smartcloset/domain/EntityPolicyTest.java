package com.smartcloset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class EntityPolicyTest {

    @Test
    void domainEntitiesDoNotUseDataOrSetterAnnotations() throws IOException {
        Path sourceRoot = Path.of("src/main/java/com/smartcloset");
        List<Path> domainFiles;
        try (var paths = Files.walk(sourceRoot)) {
            domainFiles = paths
                    .filter(path -> path.toString().contains("/domain/"))
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }

        for (Path domainFile : domainFiles) {
            String source = Files.readString(domainFile);
            assertThat(source)
                    .as(domainFile.toString())
                    .doesNotContain("@Data")
                    .doesNotContain("@Setter");
        }
    }
}
