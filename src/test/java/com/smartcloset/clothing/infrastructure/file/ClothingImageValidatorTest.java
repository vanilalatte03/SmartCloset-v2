package com.smartcloset.clothing.infrastructure.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ClothingImageValidatorTest {

    private ClothingImageValidator validator;

    @BeforeEach
    void setUp() {
        ClothingImageProperties properties = new ClothingImageProperties();
        properties.setMaxSizeBytes(12);
        validator = new ClothingImageValidator(properties);
    }

    @Test
    void validatesJpegPngAndWebpImages() {
        assertThat(validator.validate(file("image.jpg", "image/jpeg", jpegBytes())).extension())
                .isEqualTo("jpg");
        assertThat(validator.validate(file("image.png", "image/png", pngBytes())).contentType())
                .isEqualTo("image/png");
        assertThat(validator.validate(file("image.webp", "image/webp", webpBytes())).sizeBytes())
                .isEqualTo(webpBytes().length);
    }

    @Test
    void rejectsEmptyFile() {
        assertInvalid(file("empty.jpg", "image/jpeg", new byte[0]));
    }

    @Test
    void rejectsOversizedFile() {
        assertInvalid(file("large.jpg", "image/jpeg", new byte[] {
                (byte) 0xff, (byte) 0xd8, (byte) 0xff, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10
        }));
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertInvalid(file("image.gif", "image/gif", new byte[] {'G', 'I', 'F'}));
    }

    @Test
    void rejectsUnsupportedMimeType() {
        assertInvalid(file("image.jpg", "application/octet-stream", jpegBytes()));
    }

    @Test
    void rejectsExtensionAndMimeTypeMismatch() {
        assertInvalid(file("image.jpg", "image/png", pngBytes()));
    }

    @Test
    void rejectsSignatureMismatch() {
        assertInvalid(file("image.jpg", "image/jpeg", new byte[] {'n', 'o', 't', 'j', 'p', 'g'}));
    }

    private void assertInvalid(MockMultipartFile file) {
        assertThatThrownBy(() -> validator.validate(file))
                .isInstanceOfSatisfying(SmartClosetException.class, exception -> {
                    assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
                    assertThat(exception.details()).hasSize(1);
                    assertThat(exception.details().getFirst().field()).isEqualTo("image");
                });
    }

    private MockMultipartFile file(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("image", filename, contentType, content);
    }

    private byte[] jpegBytes() {
        return new byte[] {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
    }

    private byte[] pngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
        };
    }

    private byte[] webpBytes() {
        return new byte[] {
                0x52, 0x49, 0x46, 0x46, 0x01, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50
        };
    }
}
