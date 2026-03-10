package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.notice.model.CouncilNotice;

public class CouncilNoticeFixture {

    public static CouncilNotice create(Council council, String title, String content) {
        return CouncilNotice.builder()
            .title(title)
            .content(content)
            .council(council)
            .build();
    }

    public static CouncilNotice create(Council council) {
        return create(council, "공지사항 제목", "공지사항 내용입니다.");
    }
}
