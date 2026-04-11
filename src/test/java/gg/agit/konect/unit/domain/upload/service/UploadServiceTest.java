package gg.agit.konect.unit.domain.upload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        MultipartFile file = mock(MultipartFile.class);
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
    @DisplayName("uploadImage는 target이 null이면 target 경로 없이 key를 생성한다")
    void uploadImageBuildsKeyWithoutTargetWhenTargetNull() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, null);

        // then
        assertThat(response.key()).matches(
            "konect/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png"
        );
        assertThat(response.key()).doesNotContain("//");
    }

    @Test
    @DisplayName("uploadImage는 image/jpeg를 .jpg 확장자로 변환한다")
    void uploadImageConvertsJpegToJpgExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "photo.jpeg",
            "image/jpeg",
            "jpeg-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".jpg");
        assertThat(response.fileUrl()).endsWith(".jpg");
    }

    @Test
    @DisplayName("uploadImage는 image/jpg를 .jpg 확장자로 변환한다")
    void uploadImageConvertsJpgToJpgExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "photo.jpg",
            "image/jpg",
            "jpg-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".jpg");
    }

    @Test
    @DisplayName("uploadImage는 image/webp를 .webp 확장자로 변환한다")
    void uploadImageConvertsWebpToWebpExtension() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "photo.webp",
            "image/webp",
            "webp-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".webp");
    }

    @Test
    @DisplayName("uploadImage는 파일 크기가 maxUploadBytes와 정확히 일치하면 업로드에 성공한다")
    void uploadImageAcceptsFileAtExactMaxSize() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            new byte[5_000]
        );

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).isNotBlank();
        assertThat(response.fileUrl()).isNotBlank();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 content-type이 null이면 INVALID_FILE_CONTENT_TYPE으로 실패한다")
    void uploadImageRejectsNullContentType() {
        // given
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L);
        when(file.getContentType()).thenReturn(null);

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.INVALID_FILE_CONTENT_TYPE
        );
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 maxUploadBytes가 null이면 용량 검증을 생략한다")
    void uploadImageSkipsSizeCheckWhenMaxUploadBytesNull() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", null),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "big.png",
            "image/png",
            new byte[1_000_000]
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).isNotBlank();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 maxUploadBytes가 0이면 용량 검증을 생략한다")
    void uploadImageSkipsSizeCheckWhenMaxUploadBytesZero() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 0L),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "big.png",
            "image/png",
            new byte[1_000_000]
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).isNotBlank();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 CDN URL에 trailing slash가 없어도 정상 동작한다")
    void uploadImageWorksWithCdnUrlWithoutTrailingSlash() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.fileUrl()).startsWith("https://cdn.konect.test/");
        assertThat(response.fileUrl()).doesNotContain("//cdn.konect.test//");
    }

    @Test
    @DisplayName("uploadImage는 prefix에 이미 trailing slash가 있으면 중복 slash 없이 key를 생성한다")
    void uploadImageHandlesPrefixWithExistingTrailingSlash() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect/", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).matches("konect/club/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png");
        assertThat(response.key()).doesNotContain("konect//");
    }

    @Test
    @DisplayName("uploadImage는 prefix에 leading과 trailing slash가 모두 있어도 정상 동작한다")
    void uploadImageHandlesPrefixWithBothLeadingAndTrailingSlash() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "/assets/", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.COUNCIL);

        // then
        assertThat(response.key()).matches("assets/council/\\d{4}-\\d{2}-\\d{2}-[0-9a-f\\-]{36}\\.png");
        assertThat(response.key()).doesNotStartWith("/");
        assertThat(response.key()).doesNotContain("//");
    }

    @Test
    @DisplayName("uploadImage는 S3Exception의 awsErrorDetails가 null이어도 FAILED_UPLOAD_FILE로 변환한다")
    void uploadImageConvertsS3ExceptionWithNullErrorDetails() {
        // given
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "logo.png",
            "image/png",
            "png-data".getBytes(StandardCharsets.UTF_8)
        );
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenThrow(S3Exception.builder().message("s3 failed").statusCode(500).build());

        // when & then
        assertCustomException(
            () -> uploadService.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.FAILED_UPLOAD_FILE
        );
    }

    @Test
    @DisplayName("uploadImage는 CDN URL이 비어 있으면 ILLEGAL_STATE로 실패한다")
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
    @DisplayName("uploadImage는 CDN URL이 null이면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenCdnBaseUrlNull() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties(null)
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

    @Test
    @DisplayName("uploadImage는 bucket이 null이면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenBucketNull() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties(null, "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        assertCustomException(
            () -> service.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.ILLEGAL_STATE
        );
    }

    @Test
    @DisplayName("uploadImage는 region이 null이면 ILLEGAL_STATE로 실패한다")
    void uploadImageFailsWhenRegionNull() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", null, "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when & then
        assertCustomException(
            () -> service.uploadImage(file, UploadTarget.CLUB),
            ApiResponseCode.ILLEGAL_STATE
        );
    }

    @Test
    @DisplayName("uploadImage는 maxUploadBytes가 음수면 용량 검증을 생략한다")
    void uploadImageSkipsSizeCheckWhenMaxUploadBytesNegative() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", -1L),
            new StorageCdnProperties("https://cdn.konect.test/")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "big.png", "image/png",
            new byte[1_000_000]
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).isNotBlank();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 CDN baseUrl에 double trailing slash가 있어도 fileUrl에 double slash가 없다")
    void uploadImageRemovesAllTrailingSlashesFromCdnBaseUrl() {
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test//")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.fileUrl()).startsWith("https://cdn.konect.test/");
        assertThat(response.fileUrl()).doesNotContain("cdn.konect.test//");
    }

    @Test
    @DisplayName("uploadImage는 대문자 content-type 'IMAGE/PNG'을 정규화하여 허용한다")
    void uploadImageAcceptsUppercaseContentType() {
        MultipartFile file = mockFile("image.png", "IMAGE/PNG", 10L);

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".png");
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("uploadImage는 혼합 대소문자 content-type 'image/PNG'을 정규화하여 허용한다")
    void uploadImageAcceptsMixedCaseContentType() {
        MultipartFile file = mockFile("image.png", "image/PNG", 10L);

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage는 content-type에 leading whitespace가 있어도 정규화하여 허용한다")
    void uploadImageAcceptsContentTypeWithLeadingWhitespace() {
        MultipartFile file = mockFile("image.png", " image/png", 10L);

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage는 content-type에 trailing whitespace가 있어도 정규화하여 허용한다")
    void uploadImageAcceptsContentTypeWithTrailingWhitespace() {
        MultipartFile file = mockFile("image.png", "image/png ", 10L);

        // when
        ImageUploadResponse response = uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.key()).endsWith(".png");
    }

    @Test
    @DisplayName("uploadImage는 fileUrl을 baseUrl + '/' + key 형식으로 정확히 생성한다")
    void uploadImageConstructsFileUrlAsBaseUrlSlashKey() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("konect-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        ImageUploadResponse response = service.uploadImage(file, UploadTarget.CLUB);

        // then
        assertThat(response.fileUrl())
            .isEqualTo("https://cdn.konect.test/" + response.key());
        assertThat(response.fileUrl()).doesNotMatch(".*://.*//.*");
    }

    @Test
    @DisplayName("uploadImage는 PutObjectRequest에 정규화된 content-type을 전달한다")
    void uploadImagePassesNormalizedContentTypeToPutObjectRequest() {
        // given — content-type이 정규화(trim + lowercase)되어 S3에 전달됨
        MockMultipartFile file = new MockMultipartFile(
            "file", "photo.jpg", "IMAGE/JPEG",
            "jpeg-data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("uploadImage는 RequestBody에 file.getSize() 값을 그대로 전달한다")
    void uploadImagePassesExactFileSizeToRequestBody() {
        // given
        byte[] content = new byte[1234];
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png", content
        );

        // when
        uploadService.uploadImage(file, UploadTarget.CLUB);

        // then
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(any(PutObjectRequest.class), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue().contentLength()).isEqualTo(1234);
    }

    @Test
    @DisplayName("uploadImage는 PutObjectRequest의 bucket이 설정값과 일치한다")
    void uploadImageUsesConfiguredBucketInPutObjectRequest() {
        // given
        UploadService service = new UploadService(
            s3Client,
            new S3StorageProperties("my-custom-bucket", "ap-northeast-2", "konect", 5_000L),
            new StorageCdnProperties("https://cdn.konect.test")
        );
        MockMultipartFile file = new MockMultipartFile(
            "file", "logo.png", "image/png",
            "data".getBytes(StandardCharsets.UTF_8)
        );

        // when
        service.uploadImage(file, UploadTarget.CLUB);

        // then
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("my-custom-bucket");
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
