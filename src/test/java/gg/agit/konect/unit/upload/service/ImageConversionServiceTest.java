package gg.agit.konect.unit.upload.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import gg.agit.konect.domain.upload.service.ImageConversionService;

import com.sksamuel.scrimage.ImmutableImage;

class ImageConversionServiceTest {

    private final ImageConversionService imageConversionService = new ImageConversionService();

    @Test
    @DisplayName("가로가 1080을 넘는 이미지는 비율을 유지한 채 1080 폭으로 축소한다")
    void convertToWebPWhenWidthExceedsLimitResizesToMaxWidth() throws Exception {
        byte[] originalBytes = createPngBytes(2160, 1080);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "wide.png",
            "image/png",
            originalBytes
        );

        ImageConversionService.ConversionResult result = imageConversionService.convertToWebP(file);
        BufferedImage convertedImage = ImmutableImage.loader().fromBytes(result.bytes()).awt();

        assertThat(result.bytes()).isNotEmpty();
        assertThat(result.bytes()).isNotEqualTo(originalBytes);
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.extension()).isEqualTo("webp");
        assertThat(convertedImage).isNotNull();
        assertThat(convertedImage.getWidth()).isEqualTo(1080);
        assertThat(convertedImage.getHeight()).isEqualTo(540);
    }

    @Test
    @DisplayName("가로가 1080을 넘는 세로형 이미지는 비율을 유지한 채 축소한다")
    void convertToWebPWhenPortraitWidthExceedsLimitResizesByRatio() throws Exception {
        byte[] originalBytes = createPngBytes(2160, 4320);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "portrait.png",
            "image/png",
            originalBytes
        );

        ImageConversionService.ConversionResult result = imageConversionService.convertToWebP(file);
        BufferedImage convertedImage = ImmutableImage.loader().fromBytes(result.bytes()).awt();

        assertThat(result.bytes()).isNotEmpty();
        assertThat(result.bytes()).isNotEqualTo(originalBytes);
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.extension()).isEqualTo("webp");
        assertThat(convertedImage).isNotNull();
        assertThat(convertedImage.getWidth()).isEqualTo(1080);
        assertThat(convertedImage.getHeight()).isEqualTo(2160);
    }

    private byte[] createPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
