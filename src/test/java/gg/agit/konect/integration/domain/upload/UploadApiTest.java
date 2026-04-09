package gg.agit.konect.integration.domain.upload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;
import gg.agit.konect.domain.upload.enums.UploadTarget;
import gg.agit.konect.support.IntegrationTestSupport;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

class UploadApiTest extends IntegrationTestSupport {

    private static final int LOGIN_USER_ID = 2024001001;
    private static final int MAX_UPLOAD_BYTES = 20 * 1024 * 1024;

    @MockitoBean
    private S3Client s3Client;

    @BeforeEach
    void setUp() throws Exception {
        mockLoginUser(LOGIN_USER_ID);
    }

    @Nested
    @DisplayName("POST /upload/image - 이미지 업로드")
    class UploadImage {

        @Test
        @DisplayName("지원하는 이미지를 업로드하면 원본 확장자로 key와 CDN URL을 반환한다")
        void uploadImageSuccess() throws Exception {
            // given
            byte[] pngBytes = createPngBytes(8, 8);
            MockMultipartFile file = imageFile("club.png", "image/png", pngBytes);

            // when
            MvcResult result = uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isOk())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            String key = JsonPath.read(responseBody, "$.key");
            String fileUrl = JsonPath.read(responseBody, "$.fileUrl");

            assertThat(key).startsWith("test/club/");
            assertThat(key).endsWith(".png");
            assertThat(fileUrl).isEqualTo("https://cdn.test.com/" + key);

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
            assertThat(requestCaptor.getValue().key()).isEqualTo(key);
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("jpeg 이미지를 업로드하면 원본 형태로 저장한다")
        void uploadJpegImageSuccess() throws Exception {
            // given
            byte[] jpegBytes = createJpegBytes(8, 8);
            MockMultipartFile file = imageFile("club.jpg", "image/jpeg", jpegBytes);

            // when
            MvcResult result = uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isOk())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            String key = JsonPath.read(responseBody, "$.key");

            assertThat(key).startsWith("test/club/");
            assertThat(key).endsWith(".jpg");

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("jpg content type 이미지를 업로드하면 원본 형태로 저장한다")
        void uploadJpgContentTypeImageSuccess() throws Exception {
            // given
            byte[] jpegBytes = createJpegBytes(8, 8);
            MockMultipartFile file = imageFile("club.jpg", "image/jpg", jpegBytes);

            // when
            MvcResult result = uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isOk())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            String key = JsonPath.read(responseBody, "$.key");

            assertThat(key).startsWith("test/club/");
            assertThat(key).endsWith(".jpg");

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/jpg");
        }

        @Test
        @DisplayName("webp 이미지를 업로드하면 원본 형태로 저장한다")
        void uploadWebpImageSuccess() throws Exception {
            // given - webp 형태로 mock (실제 webp 변환 없이 단순 bytes로 처리)
            byte[] webpBytes = new byte[] {0x52, 0x49, 0x46, 0x46}; // RIFF header mock
            MockMultipartFile file = imageFile("club.webp", "image/webp", webpBytes);

            // when
            MvcResult result = uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isOk())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            String key = JsonPath.read(responseBody, "$.key");

            assertThat(key).startsWith("test/club/");
            assertThat(key).endsWith(".webp");

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("큰 이미지도 원본 형태로 업로드한다 (리사이징 없음)")
        void uploadLargeImageWithoutResizing() throws Exception {
            // given
            byte[] pngBytes = createPngBytes(2160, 1080);
            MockMultipartFile file = imageFile("wide.png", "image/png", pngBytes);

            // when
            MvcResult result = uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isOk())
                .andReturn();

            // then
            String responseBody = result.getResponse().getContentAsString();
            String key = JsonPath.read(responseBody, "$.key");

            assertThat(key).startsWith("test/club/");
            assertThat(key).endsWith(".png");

            ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
            ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
            verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());
            assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        }

        @Test
        @DisplayName("빈 파일을 업로드하면 400을 반환한다")
        void uploadEmptyFileFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("empty.png", "image/png", new byte[0]);

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("허용하지 않는 content type이면 400을 반환한다")
        void uploadImageWithInvalidContentTypeFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("note.txt", "text/plain", "not-image".getBytes());

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_CONTENT_TYPE"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("content type 이 없으면 400을 반환한다")
        void uploadImageWithoutContentTypeFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", null, createPngBytes(8, 8));

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_CONTENT_TYPE"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("content type 이 비어 있으면 400을 반환한다")
        void uploadImageWithBlankContentTypeFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", " ", createPngBytes(8, 8));

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILE_CONTENT_TYPE"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("최대 업로드 크기를 넘기면 413을 반환한다")
        void uploadImageWithTooLargeFileFails() throws Exception {
            // given
            MockMultipartFile file = imageFile(
                "large.png",
                "image/png",
                new byte[MAX_UPLOAD_BYTES + 1]
            );

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("target 파라미터가 없으면 400을 반환한다")
        void uploadImageWithoutTargetFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", "image/png", createPngBytes(8, 8));

            // when & then
            uploadImageWithoutTarget(file)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MISSING_REQUIRED_PARAMETER"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("지원하지 않는 target 이면 400을 반환한다")
        void uploadImageWithInvalidTargetFails() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", "image/png", createPngBytes(8, 8));

            // when & then
            uploadImage(file, "INVALID")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TYPE_VALUE"));

            verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("S3 업로드에 실패하면 500을 반환한다")
        void uploadImageWhenS3FailsReturnsInternalServerError() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", "image/png", createPngBytes(8, 8));
            willThrow(S3Exception.builder().statusCode(500).message("upload failed").build())
                .given(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FAILED_UPLOAD_FILE"));
        }

        @Test
        @DisplayName("S3 클라이언트 오류가 발생하면 500을 반환한다")
        void uploadImageWhenS3ClientFailsReturnsInternalServerError() throws Exception {
            // given
            MockMultipartFile file = imageFile("club.png", "image/png", createPngBytes(8, 8));
            willThrow(SdkClientException.create("network failure"))
                .given(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

            // when & then
            uploadImage(file, UploadTarget.CLUB)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FAILED_UPLOAD_FILE"));
        }
    }

    private ResultActions uploadImage(MockMultipartFile file, UploadTarget target) throws Exception {
        return uploadImage(file, target.name());
    }

    private ResultActions uploadImage(MockMultipartFile file, String target) throws Exception {
        return mockMvc.perform(
            multipart("/upload/image")
                .file(file)
                .param("target", target)
        );
    }

    private ResultActions uploadImageWithoutTarget(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/upload/image")
                .file(file)
        );
    }

    private MockMultipartFile imageFile(String fileName, String contentType, byte[] bytes) {
        return new MockMultipartFile("file", fileName, contentType, bytes);
    }

    private byte[] createPngBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return outputStream.toByteArray();
        }
    }

    private byte[] createJpegBytes(int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", outputStream);
            return outputStream.toByteArray();
        }
    }
}
