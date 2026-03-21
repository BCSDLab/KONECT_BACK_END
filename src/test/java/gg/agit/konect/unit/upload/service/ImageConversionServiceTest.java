package gg.agit.konect.unit.upload.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.sksamuel.scrimage.AwtImage;

import gg.agit.konect.domain.upload.service.ImageConversionService;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;

class ImageConversionServiceTest {

    private final ImageConversionService imageConversionService = new ImageConversionService();

    @Test
    @DisplayName("가로가 1800을 넘는 이미지는 비율을 유지한 채 1800 폭으로 축소한다")
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
        assertThat(convertedImage.getWidth()).isEqualTo(1800);
        assertThat(convertedImage.getHeight()).isEqualTo(900);
    }

    @Test
    @DisplayName("가로가 1800을 넘는 세로형 이미지는 비율을 유지한 채 축소한다")
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
        assertThat(convertedImage.getWidth()).isEqualTo(1800);
        assertThat(convertedImage.getHeight()).isEqualTo(3600);
    }

    @Test
    @DisplayName("가로가 1800을 넘는 webp 이미지는 비율을 유지한 채 1800 폭으로 축소한다")
    void convertToWebPWhenWebpWidthExceedsLimitResizesToMaxWidth() throws Exception {
        byte[] originalBytes = createWebpBytes(2160, 1080);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "wide.webp",
            "image/webp",
            originalBytes
        );

        ImageConversionService.ConversionResult result = imageConversionService.convertToWebP(file);
        BufferedImage convertedImage = ImmutableImage.loader().fromBytes(result.bytes()).awt();

        assertThat(result.bytes()).isNotEmpty();
        assertThat(result.bytes()).isNotEqualTo(originalBytes);
        assertThat(result.contentType()).isEqualTo("image/webp");
        assertThat(result.extension()).isEqualTo("webp");
        assertThat(convertedImage).isNotNull();
        assertThat(convertedImage.getWidth()).isEqualTo(1800);
        assertThat(convertedImage.getHeight()).isEqualTo(900);
    }

    @Test
    @DisplayName("투명 PNG가 리사이즈 경로를 타도 알파 채널을 유지한다")
    void convertToWebPWhenTransparentPngResizesPreservesAlpha() throws Exception {
        byte[] originalBytes = createTransparentPngBytes(2160, 1080);
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "transparent.png",
            "image/png",
            originalBytes
        );

        ImageConversionService.ConversionResult result = imageConversionService.convertToWebP(file);
        BufferedImage convertedImage = ImmutableImage.loader().fromBytes(result.bytes()).awt();

        assertThat(convertedImage).isNotNull();
        assertThat(convertedImage.getColorModel().hasAlpha()).isTrue();
        assertThat(convertedImage.getWidth()).isEqualTo(1800);
        assertThat(convertedImage.getHeight()).isEqualTo(900);
        assertThat((convertedImage.getRGB(0, 0) >>> 24) & 0xff).isEqualTo(0);
    }

    private byte[] createPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createWebpBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        return new AwtImage(image).bytes(WebpWriter.DEFAULT);
    }

    private byte[] createTransparentPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(width / 2, height / 2, 0xFFFF0000);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }
}
