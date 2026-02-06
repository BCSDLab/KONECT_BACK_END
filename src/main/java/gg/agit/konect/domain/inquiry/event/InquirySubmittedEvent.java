package gg.agit.konect.domain.inquiry.event;

public record InquirySubmittedEvent(
    String content
) {
    public static InquirySubmittedEvent from(String content) {
        return new InquirySubmittedEvent(content);
    }
}
