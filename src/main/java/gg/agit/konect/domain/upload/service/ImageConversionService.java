package gg.agit.konect.domain.upload.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

/**
 * 이미지 포맷 변환 서비스 - PNG/JPG를 WEBP로 변환.
 */
@Service
@Slf4j
public class ImageConversionService {

    private static final Set<String> SKIP_CONVERSION_TYPES = Set.of("image/webp");

    private static final float DEFAULT_WEBP_QUALITY = 0.8f;

    public ConversionResult convertToWebP(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType != null && SKIP_CONVERSION_TYPES.contains(contentType.toLowerCase())) {
            log.debug("WEBP 이미지는 변환을 건너뜁니다: contentType={}", contentType);
            return new ConversionResult(file.getBytes(), contentType, getExtension(contentType));
        }

        try (InputStream input = file.getInputStream()) {
            BufferedImage image = ImageIO.read(input);

            if (image == null) {
                throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
            }

            byte[] webpBytes = convertImageToWebP(image, DEFAULT_WEBP_QUALITY);
            log.info("이미지 WEBP 변환 완료: 원본 {} bytes → WEBP {} bytes", file.getSize(), webpBytes.length);

            return new ConversionResult(webpBytes, "image/webp", "webp");
        }
    }

    private byte[] convertImageToWebP(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("webp").next();
            writer.setOutput(ios);

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(quality);
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
            writer.dispose();
        }

        return output.toByteArray();
    }

    private String getExtension(String contentType) {
        return switch (contentType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpg", "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    public record ConversionResult(byte[] bytes, String contentType, String extension) {
    }
}
