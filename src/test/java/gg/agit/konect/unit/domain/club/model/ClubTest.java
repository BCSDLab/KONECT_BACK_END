package gg.agit.konect.unit.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_REQUEST_BODY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;

class ClubTest extends ServiceTestSupport {

    @Test
    @DisplayName("replaceFeeInfo는 네 필드가 모두 비면 기존 회비 정보를 전부 제거한다")
    void replaceFeeInfoClearsAllFeeFieldsWhenEveryFieldIsBlank() {
        // given
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 1, "BCSD");
        club.replaceFeeInfo("30000", "국민은행", "123-456-7890", "BCSD");

        // when
        club.replaceFeeInfo(null, null, null, null);

        // then
        assertThat(club.getFeeAmount()).isNull();
        assertThat(club.getFeeBank()).isNull();
        assertThat(club.getFeeAccountNumber()).isNull();
        assertThat(club.getFeeAccountHolder()).isNull();
    }

    @Test
    @DisplayName("replaceFeeInfo는 일부만 비어 있는 partial 입력을 거부한다")
    void replaceFeeInfoRejectsPartialInput() {
        // given
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 1, "BCSD");

        // when & then
        assertThatThrownBy(() -> club.replaceFeeInfo("30000", "국민은행", null, null))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(INVALID_REQUEST_BODY));
    }
}
