package gg.agit.konect.infrastructure.slack.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SlackMessageTemplate {

    USER_REGISTER(
        """
        `%s님이 가입하셨습니다.`
        """
    ),
    USER_WITHDRAWAL(
        """
        `%s님이 탈퇴하셨습니다.`
        """
    ),
    ;

    private final String template;

    public String format(Object... args) {
        return String.format(template, args);
    }
}
