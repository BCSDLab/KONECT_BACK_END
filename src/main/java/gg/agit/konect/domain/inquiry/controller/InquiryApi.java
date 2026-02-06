package gg.agit.konect.domain.inquiry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.inquiry.dto.InquiryRequest;
import gg.agit.konect.global.auth.annotation.PublicApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Inquiry: 문의", description = "문의 API")
@RequestMapping("/inquiries")
public interface InquiryApi {

    @Operation(summary = "어드민에게 문의를 전송한다.")
    @PostMapping
    @PublicApi
    ResponseEntity<Void> submitInquiry(@Valid @RequestBody InquiryRequest request);
}
