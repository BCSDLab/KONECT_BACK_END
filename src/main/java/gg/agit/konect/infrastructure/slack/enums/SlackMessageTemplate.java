package gg.agit.konect.infrastructure.slack.enums;

import lombok.Getter;

@Getter
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
    INQUIRY(
        """
        %s
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
