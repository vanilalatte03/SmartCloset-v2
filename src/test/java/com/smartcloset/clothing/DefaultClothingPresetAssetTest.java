package com.smartcloset.clothing;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DefaultClothingPresetAssetTest {

    private static final int MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final List<String> PRESET_IMAGE_FILENAMES = List.of(
            "white-short-sleeve-tshirt.jpg",
            "black-short-sleeve-tshirt.jpg",
            "black-denim-jeans.jpg",
            "dark-blue-denim-jeans.jpg",
            "black-cardigan.jpg"
    );

    @Test
    void defaultPresetImagesAreJpegAndWithinUploadLimit() throws IOException {
        for (String filename : PRESET_IMAGE_FILENAMES) {
            ClassPathResource resource = new ClassPathResource("default-clothing-presets/" + filename);

            assertThat(resource.exists()).as(filename).isTrue();
            byte[] bytes;
            try (InputStream inputStream = resource.getInputStream()) {
                bytes = inputStream.readAllBytes();
            }

            assertThat(bytes).as(filename).hasSizeGreaterThan(3);
            assertThat(bytes.length).as(filename).isLessThanOrEqualTo(MAX_IMAGE_SIZE_BYTES);
            assertThat(URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes)))
                    .as(filename)
                    .isEqualTo("image/jpeg");
            assertThat(bytes[0] & 0xff).as(filename).isEqualTo(0xff);
            assertThat(bytes[1] & 0xff).as(filename).isEqualTo(0xd8);
            assertThat(bytes[2] & 0xff).as(filename).isEqualTo(0xff);
        }
    }
}
