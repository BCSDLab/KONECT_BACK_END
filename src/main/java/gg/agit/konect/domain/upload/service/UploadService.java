package gg.agit.konect.domain.upload.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.domain.upload.enums.UploadTarget;
import gg.agit.konect.domain.upload.service.ImageConversionService.ConversionResult;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.storage.cdn.StorageCdnProperties;
import gg.agit.konect.infrastructure.storage.s3.S3StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Slf4j
@RequiredArgsConstructor
public class UploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "image/png",
        "image/jpg",
        "image/jpeg",
        "image/webp",
        "image/avif"
    );

    private final S3Client s3Client;
    private final S3StorageProperties s3StorageProperties;
    private final StorageCdnProperties storageCdnProperties;
    private final ImageConversionService imageConversionService;

    public ImageUploadResponse uploadImage(MultipartFile file, UploadTarget target) {
        validateS3Configuration();
        validateFile(file);

        ConversionResult conversionResult;
        try {
            conversionResult = imageConversionService.convertToWebP(file);
        } catch (IOException e) {
            log.error("이미지 WEBP 변환 실패. fileName: {}", file.getOriginalFilename(), e);
            throw CustomException.of(ApiResponseCode.FAILED_UPLOAD_FILE);
        }

        String key = buildKey(conversionResult.extension(), target);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3StorageProperties.bucket())
            .key(key)
            .contentType(conversionResult.contentType())
            .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(conversionResult.bytes()));
        } catch (S3Exception e) {
            String awsErrorCode = e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : null;
            String awsErrorMessage = e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();

            log.error(
                "S3 업로드 실패. bucket: {}, key: {}, statusCode: {}, errorCode: {}, requestId: {}, message: {}",
                s3StorageProperties.bucket(),
                key,
                e.statusCode(),
                awsErrorCode,
                e.requestId(),
                awsErrorMessage,
                e
            );
            throw CustomException.of(ApiResponseCode.FAILED_UPLOAD_FILE);
        } catch (SdkClientException e) {
            log.error(
                "S3 업로드 클라이언트 오류(네트워크/자격증명/설정). bucket: {}, key: {}, message: {}",
                s3StorageProperties.bucket(),
                key,
                e.getMessage(),
                e
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
        if (contentType == null || contentType.isBlank() || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw CustomException.of(ApiResponseCode.INVALID_FILE_CONTENT_TYPE);
        }

        Long maxUploadBytes = s3StorageProperties.maxUploadBytes();
        if (maxUploadBytes != null && file.getSize() > maxUploadBytes) {
            throw CustomException.of(ApiResponseCode.INVALID_FILE_SIZE);
        }
    }

    private String buildKey(String extension, UploadTarget target) {
        String prefix = normalizePrefix(s3StorageProperties.keyPrefix());
        String targetPath = target != null ? target.name().toLowerCase() : "";
        LocalDate today = LocalDate.now();
        String datePath = String.format(
            "%04d-%02d-%02d",
            today.getYear(),
            today.getMonthValue(),
            today.getDayOfMonth()
        );
        String uuid = UUID.randomUUID().toString();

        if (targetPath.isEmpty()) {
            return prefix + datePath + "-" + uuid + "." + extension;
        }

        return prefix + targetPath + "/" + datePath + "-" + uuid + "." + extension;
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
