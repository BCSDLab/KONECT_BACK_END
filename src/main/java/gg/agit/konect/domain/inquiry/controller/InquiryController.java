package gg.agit.konect.domain.inquiry.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.inquiry.dto.InquiryRequest;
import gg.agit.konect.domain.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inquiries")
public class InquiryController implements InquiryApi {

    private final InquiryService inquiryService;

    @Override
    public ResponseEntity<Void> submitInquiry(InquiryRequest request) {
        inquiryService.submitInquiry(request.content());
        return ResponseEntity.ok().build();
    }
}
