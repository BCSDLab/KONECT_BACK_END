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
    CLUB_REGISTRATION_REQUEST(
        """
            :sparkles: *새 동아리 등록 요청이 도착했어요*

            :school: *대학교* : *`%s`*
            %s *동아리* : *`%s`*
            :label: *분과* : *`%s`*
            :dart: *주제* : *`%s`*
            :art: *요청 이모지* : *`%s`*

            :memo: *한 줄 소개*
            ```%s```

            :page_facing_up: *상세 소개*
            ```%s```

            :paperclip: *첨부 이미지*
            ```%s```
            """
    ),
    CLUB_INFORMATION_UPDATE_REQUEST(
        """
            :pencil2: *동아리 정보 수정 요청이 도착했어요*

            :receipt: *요청 ID* : *`%s`*
            :id: *동아리 ID* : *`%s`*
            :school: *대학교* : *`%s`* → *`%s`*
            :bookmark: *동아리명* : *`%s`* → *`%s`*
            :label: *분과* : *`%s`* → *`%s`*
            :dart: *주제* : *`%s`* → *`%s`*
            :art: *요청 이모지* : *`%s`*

            :memo: *한 줄 소개*
            ```%s```
            →
            ```%s```

            :page_facing_up: *상세 소개*
            ```%s```
            →
            ```%s```

            :frame_with_picture: *현재 대표 이미지*
            ```%s```

            :paperclip: *요청 첨부 이미지*
            ```%s```
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
