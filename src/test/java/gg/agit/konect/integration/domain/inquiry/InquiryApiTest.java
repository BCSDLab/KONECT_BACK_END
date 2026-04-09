package gg.agit.konect.integration.domain.inquiry;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.inquiry.dto.InquiryRequest;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;

class InquiryApiTest extends IntegrationTestSupport {

    private static final String INQUIRIES_ENDPOINT = "/inquiries";

    @Nested
    @DisplayName("POST /inquiries - 문의 전송")
    class SubmitInquiry {

        @Test
        @DisplayName("문의 내용을 전송한다")
        void submitInquirySuccess() throws Exception {
            // given
            InquiryRequest request = new InquiryRequest("앱 사용 중 오류가 발생했습니다.");

            // when & then
            performPost(INQUIRIES_ENDPOINT, request)
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("문의 내용이 비어 있으면 400을 반환한다")
        void submitInquiryWithBlankContentFails() throws Exception {
            // given
            InquiryRequest request = new InquiryRequest(" ");

            // when & then
            performPost(INQUIRIES_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }
    }
}
