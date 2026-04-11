package gg.agit.konect.unit.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import gg.agit.konect.domain.upload.dto.ImageUploadResponse;
import gg.agit.konect.domain.upload.enums.UploadTarget;
import gg.agit.konect.domain.upload.service.UploadService;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.infrastructure.storage.cdn.StorageCdnProperties;
import gg.agit.konect.infrastructure.storage.s3.S3StorageProperties;
import gg.agit.konect.support.ServiceTestSupport;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class UploadServiceTest extends ServiceTestSupport {

    private static final Pattern CLUB_KEY_PATTERN = Pattern.compile(
        "(?:[\\w-]+/)*club/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png"
    );

    @Mock
    private S3Client s3Client;

    private UploadService uploadService;

    @BeforeEach
    void setUp() {
        uploadService = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
    }

    @Test
    @DisplayName("uploadImage는 유효한 PNG 파일을 업로드하고 key와 CDN URL을 반환한다")
    void uploadImageUploadsPngAndReturnsKeyAndCdnUrl() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertThat(request.bucket()).isEqualTo("konect-bucket");
        assertThat(request.contentType()).isEqualTo("image/png");
        assertThat(request.key()).matches(CLUB_KEY_PATTERN);
        assertThat(response.key()).isEqualTo(request.key());
        assertThat(response.fileUrl()).isEqualTo("https://cdn.konect.test/" + request.key());
    }

    @Test
    @DisplayName("uploadImage는 blank prefix여도 target 경로만 포함한 key를 만든다")
    void uploadImageBuildsKeyWithoutPrefixWhenPrefixBlank() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", " ", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "bank.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.BANK);

        // then
        assertThat(response.key()).matches("bank/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png");
    }

    @Test
    @DisplayName("uploadImage는 leading slash prefix를 제거하고 trailing slash를 보정한다")
    void uploadImageNormalizesPrefixWithLeadingSlash() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "/assets", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "user.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.USER);

        // then
        assertThat(response.key()).matches("assets/user/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png");
        assertThat(response.key()).doesNotStartWith("/");
    }

    @Test
    @DisplayName("uploadImage는 null 파일을 거부한다")
    void uploadImageRejectsNullFile() {
        assertCustomException(
            () -> uploadService.uploadImage(null, UploadTarget.CLUB),
            ApiResponseCode.INVALID_REQUEST_BODY
        );
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 empty 파일을 거부한다")
    void uploadImageRejectsEmptyFile() {
        // given
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.INVALID_REQUEST_BODY
        );
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 blank content-type을 거부한다")
    void uploadImageRejectsBlankContentType() {
        // given
        MultipartFile file = mockFile("image.png", " ", 10L);

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.INVALID_FILE_CONTENT_TYPE
        );
    }

    @Test
    @DisplayName("uploadImage는 허용되지 않은 content-type을 거부한다")
    void uploadImageRejectsUnsupportedContentType() {
        // given
        MultipartFile file = mockFile("document.pdf", "application/pdf", 10L);

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.INVALID_FILE_CONTENT_TYPE
        );
    }

    @Test
    @DisplayName("uploadImage는 maxUploadBytes를 초과하면 PAYLOAD_TOO_LARGE로 실패한다")
    void uploadImageRejectsOversizedFile() {
        // given
        MultipartFile file = mockFile("large.png", "image/png", 5_001L);

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.PAYLOAD_TOO_LARGE
        );
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 S3Exception을 FAILED_UPLOAD_FILE로 변환한다")
    void uploadImageConvertsS3Exception() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("s3 failed").build());

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.FAILED_UPLOAD_FILE
        );
    }

    @Test
    @DisplayName("uploadImage는 SdkClientException을 FAILED_UPLOAD_FILE로 변환한다")
    void uploadImageConvertsSdkClientException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(SdkClientException.create("client failed"));

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.FAILED_UPLOAD_FILE
        );
    }

    @Test
    @DisplayName("uploadImage는 InputStream IOException을 FAILED_UPLOAD_FILE로 변환한다")
    void uploadImageConvertsIOException() throws IOException {
        // given
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getContentType()).thenReturn("image/png");
        when(file.getInputStream()).thenThrow(new IOException("stream failed"));

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.FAILED_UPLOAD_FILE
        );
    }

    @Test
    @DisplayName("uploadImage는 CDN base URL이 비어 있으면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenCdnBaseUrlMissing() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties(" ")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        assertCustomException(() -> service.uploadImage(file, UploadTarget.CLUB), ApiResponseCode.ILLEGAL_STATE);
    }

    @Test
    @DisplayName("uploadImage는 bucket 설정이 비어 있으면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenBucketMissing() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties(" ", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        assertCustomException(() -> service.uploadImage(file, UploadTarget.CLUB), ApiResponseCode.ILLEGAL_STATE);
    }

    @Test
    @DisplayName("uploadImage는 region 설정이 비어 있으면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenRegionMissing() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", " ", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        assertCustomException(() -> service.uploadImage(file, UploadTarget.CLUB), ApiResponseCode.ILLEGAL_STATE);
    }

    private MultipartFile mockFile(String filename, String contentType, long size) {
        return new MockMultipartFile(
            "file",
            filename,
            contentType,
            new byte[Math.toIntExact(size)]
        );
    }

    private void assertCustomException(ThrowingCallable callable, ApiResponseCode expectedCode) {
        assertThatThrownBy(callable::call)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(expectedCode));
    }

    @FunctionalInterface
    private interface ThrowingCallable {
        void call() throws Exception;
    }
}
