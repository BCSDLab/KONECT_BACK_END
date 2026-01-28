package gg.agit.konect.domain.upload.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.config.S3StorageProperties;
import gg.agit.konect.global.config.StorageCdnProperties;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpeg",
        "image/webp"
    );

    private final S3Client s3Client;
    private final S3StorageProperties s3StorageProperties;
    private final StorageCdnProperties storageCdnProperties;

    public ImageUploadResponse uploadImage(MultipartFile file) {
        validateS3Configuration();
        validateFile(file);

        String contentType = requireContentType(file);
        String extension = extensionFromContentType(contentType);
        String key = buildKey(extension);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3StorageProperties.bucket())
            .key(key)
            .contentType(contentType)
            .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
        } catch (IOException e) {
            log.warn(
                "파일 업로드중 문제가 발생했습니다. fileName: {}, fileSize: {}, contentType: {} \n message: {}",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType(),
                e.getMessage()
            );
            throw CustomException.of(ApiResponseCode.FAILED_UPLOAD_FILE);
        }

        String fileUrl = trimTrailingSlash(storageCdnProperties.baseUrl()) + "/" + key;
        return new ImageUploadResponse(key, fileUrl);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw CustomException.of(ApiResponseCode.INVALID_REQUEST_BODY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
        }

        Long maxUploadBytes = s3StorageProperties.maxUploadBytes();
        if (maxUploadBytes != null && file.getSize() > maxUploadBytes) {
            throw CustomException.of(ApiResponseCode.INVALID_FILE_SIZE);
        }
    }

    private String requireContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
        }
        return contentType;
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/webp" -> "webp";
            default -> throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
        };
    }

    private String buildKey(String extension) {
        String prefix = normalizePrefix(s3StorageProperties.keyPrefix());
        LocalDate today = LocalDate.now();
        String datePath = String.format(
            "%04d-%02d-%02d",
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth()
        );
        String uuid = UUID.randomUUID().toString();
        return prefix + datePath + "-" + uuid + "." + extension;
    }

    private String normalizePrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "";
        }

        String normalized = keyPrefix.trim();
        if (!normalized.endsWith("/")) {
            normalized += "/";
        }
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw CustomException.of(ApiResponseCode.ILLEGAL_STATE, "storage.cdn.base-url 설정이 필요합니다.");
        }

        String trimmed = baseUrl.trim();

        if (trimmed.endsWith("/")) {
            return trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed;
    }

    private void validateS3Configuration() {
        if (s3StorageProperties.bucket() == null || s3StorageProperties.bucket().isBlank()) {
            throw CustomException.of(ApiResponseCode.ILLEGAL_STATE, "storage.s3.bucket 설정이 필요합니다.");
        }

        if (s3StorageProperties.region() == null || s3StorageProperties.region().isBlank()) {
            throw CustomException.of(ApiResponseCode.ILLEGAL_STATE, "storage.s3.region 설정이 필요합니다.");
        }
    }
}
