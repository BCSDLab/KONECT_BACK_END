package gg.agit.konect.domain.upload.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
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

    /**
     * 이미지 최대 해상도 (가로/세로 중 큰 값).
     * 8000픽셀로 제한하여 메모리 사용량을 256MB(8000×8000×4bytes) 수준으로 유지.
     */
    private static final int MAX_IMAGE_DIMENSION = 8000;

    public ConversionResult convertToWebP(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType != null && SKIP_CONVERSION_TYPES.contains(contentType.toLowerCase())) {
            log.debug("WEBP 이미지는 변환을 건너뜁니다: contentType={}", contentType);
            return new ConversionResult(file.getBytes(), contentType, getExtension(contentType));
        }

        try (InputStream input = file.getInputStream();
             ImageInputStream iis = ImageIO.createImageInputStream(input)) {

            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
            }

            ImageReader reader = readers.next();
            try {
                validateImageDimensions(reader, iis);

                ImageReadParam readParam = reader.getDefaultReadParam();
                BufferedImage image = reader.read(0, readParam);

                if (image == null) {
                    throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
                }

                byte[] webpBytes = convertImageToWebP(image, DEFAULT_WEBP_QUALITY);
                log.info("이미지 WEBP 변환 완료: 원본 {} bytes → WEBP {} bytes", file.getSize(), webpBytes.length);

                return new ConversionResult(webpBytes, "image/webp", "webp");
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateImageDimensions(ImageReader reader, ImageInputStream iis) throws IOException {
        reader.setInput(iis);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);

        if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
            log.warn("이미지 해상도 초과: {}x{} (최대 {}px)", width, height, MAX_IMAGE_DIMENSION);
            throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
        }
    }

    private byte[] convertImageToWebP(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("webp");
            if (!writers.hasNext()) {
                throw new IllegalStateException(
                    "WEBP ImageWriter를 찾을 수 없습니다. TwelveMonkeys imageio-webp 플러그인이 로드되었는지 확인하세요.");
            }

            ImageWriter writer = writers.next();
            try {
                writer.setOutput(ios);

                ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(quality);
                }

                writer.write(null, new IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }
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
