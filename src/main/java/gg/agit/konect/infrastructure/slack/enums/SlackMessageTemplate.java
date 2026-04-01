package gg.agit.konect.infrastructure.slack.enums;

import lombok.Getter;

@Getter
public enum SlackMessageTemplate {

    USER_REGISTER(
        """
        `%s님이 가입하셨습니다. Provider : %s`
        """
    ),
    USER_WITHDRAWAL(
        """
        `%s님이 탈퇴하셨습니다. Provider : %s`
        """
    ),
    INQUIRY(
        """
        *:incoming_envelope: 사용자로부터 문의가 도착했습니다.*
        > %s
        """
    ),
    ADMIN_CHAT_RECEIVED(
        """
        *:speech_balloon: 새로운 채팅이 도착했습니다.*
        보낸 사람: %s
        > %s
        """
    ),
    SHEET_SYNC_FAILED(
        """
        *:warning: 시트 동기화 실패*
        동아리 ID: %s
        스프레드시트 ID: `%s`
        유형: %s
        발생 시각: %s
        > %s
        """
    ),
    ;

    private final String template;

    SlackMessageTemplate(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }
}
