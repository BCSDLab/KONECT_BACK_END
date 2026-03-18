package gg.agit.konect.domain.upload.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sksamuel.scrimage.AwtImage;
import com.sksamuel.scrimage.webp.WebpWriter;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ImageConversionService {

    private static final Set<String> SKIP_CONVERSION_TYPES = Set.of("image/webp");

    private static final float DEFAULT_WEBP_QUALITY = 0.8f;
    private static final int WEBP_QUALITY_PERCENT_SCALE = 100;

    private static final int MAX_IMAGE_DIMENSION = 8000;

    private static final int ORIENTATION_NORMAL = 1;
    private static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    private static final int ORIENTATION_ROTATE_180 = 3;
    private static final int ORIENTATION_FLIP_VERTICAL = 4;
    private static final int ORIENTATION_ROTATE_90_FLIP = 5;
    private static final int ORIENTATION_ROTATE_90 = 6;
    private static final int ORIENTATION_ROTATE_270_FLIP = 7;
    private static final int ORIENTATION_ROTATE_270 = 8;

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

                image = applyExifOrientation(reader, image);

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

    private BufferedImage applyExifOrientation(ImageReader reader, BufferedImage image) {
        try {
            IIOMetadata metadata = reader.getImageMetadata(0);
            int orientation = readExifOrientation(metadata);
            if (orientation > ORIENTATION_NORMAL) {
                log.debug("EXIF Orientation 적용: {}", orientation);
                return rotateImage(image, orientation);
            }
        } catch (Exception e) {
            log.debug("EXIF Orientation 읽기 실패, 원본 유지: {}", e.getMessage());
        }
        return image;
    }

    private int readExifOrientation(IIOMetadata metadata) {
        for (String formatName : metadata.getMetadataFormatNames()) {
            IIOMetadataNode root = (IIOMetadataNode)metadata.getAsTree(formatName);
            Integer orientation = findOrientationInNode(root);
            if (orientation != null) {
                return orientation;
            }
        }
        return 1;
    }

    private Integer findOrientationInNode(IIOMetadataNode node) {
        if ("exif".equalsIgnoreCase(node.getNodeName()) || "Orientation".equalsIgnoreCase(node.getNodeName())) {
            String attr = node.getAttribute("value");
            if (attr.isEmpty()) {
                attr = node.getAttribute("Orientation");
            }
            if (!attr.isEmpty()) {
                try {
                    return Integer.parseInt(attr);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        String tagName = node.getAttribute("TagName");
        if ("Orientation".equals(tagName)) {
            String attr = node.getAttribute("TagValue");
            if (attr != null && !attr.isEmpty()) {
                try {
                    return Integer.parseInt(attr);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (IIOMetadataNode child = (IIOMetadataNode)node.getFirstChild();
             child != null;
             child = (IIOMetadataNode)child.getNextSibling()) {
            Integer result = findOrientationInNode(child);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private BufferedImage rotateImage(BufferedImage image, int orientation) {
        return switch (orientation) {
            case ORIENTATION_FLIP_HORIZONTAL -> flipHorizontal(image);
            case ORIENTATION_ROTATE_180 -> rotate180(image);
            case ORIENTATION_FLIP_VERTICAL -> flipVertical(image);
            case ORIENTATION_ROTATE_90_FLIP -> flipHorizontal(rotate90(image));
            case ORIENTATION_ROTATE_90 -> rotate90(image);
            case ORIENTATION_ROTATE_270_FLIP -> flipHorizontal(rotate270(image));
            case ORIENTATION_ROTATE_270 -> rotate270(image);
            default -> image;
        };
    }

    private BufferedImage rotate90(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage rotated = new BufferedImage(h,
            w,
            image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D g = rotated.createGraphics();
        g.translate((h - w) / 2, (h - w) / 2);
        g.rotate(Math.PI / 2, h / 2.0, w / 2.0);
        g.drawRenderedImage(image, null);
        g.dispose();
        return rotated;
    }

    private BufferedImage rotate180(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage rotated = new BufferedImage(w,
            h,
            image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D g = rotated.createGraphics();
        g.rotate(Math.PI, w / 2.0, h / 2.0);
        g.drawRenderedImage(image, null);
        g.dispose();
        return rotated;
    }

    private BufferedImage rotate270(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage rotated = new BufferedImage(h,
            w,
            image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D g = rotated.createGraphics();
        g.translate((h - w) / 2, (h - w) / 2);
        g.rotate(-Math.PI / 2, h / 2.0, w / 2.0);
        g.drawRenderedImage(image, null);
        g.dispose();
        return rotated;
    }

    private BufferedImage flipHorizontal(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage flipped = new BufferedImage(w,
            h,
            image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D g = flipped.createGraphics();
        g.drawImage(image, w, 0, -w, h, null);
        g.dispose();
        return flipped;
    }

    private BufferedImage flipVertical(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage flipped = new BufferedImage(w,
            h,
            image.getType() == 0 ? BufferedImage.TYPE_INT_RGB : image.getType());
        Graphics2D g = flipped.createGraphics();
        g.drawImage(image, 0, h, w, -h, null);
        g.dispose();
        return flipped;
    }

    private byte[] convertImageToWebP(BufferedImage image, float quality) throws IOException {
        try {
            return new AwtImage(image)
                .bytes(WebpWriter.DEFAULT.withQ(toWebpQualityPercent(quality)));
        } catch (RuntimeException e) {
            throw new IOException("WEBP 이미지 변환에 실패했습니다.", e);
        }
    }

    private int toWebpQualityPercent(float quality) {
        if (quality <= 0) {
            return 0;
        }
        if (quality >= 1) {
            return WEBP_QUALITY_PERCENT_SCALE;
        }
        return Math.round(quality * WEBP_QUALITY_PERCENT_SCALE);
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
