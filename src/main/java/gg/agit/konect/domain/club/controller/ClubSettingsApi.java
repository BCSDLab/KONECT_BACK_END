package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubSettingsResponse;
import gg.agit.konect.domain.club.dto.ClubSettingsUpdateRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Settings: 동아리 설정 관리")
@RequestMapping("/clubs")
public interface ClubSettingsApi {

    @Operation(summary = "동아리 설정 정보를 조회한다.", description = """
        동아리 설정 관리 화면에 필요한 정보를 조회합니다.

        토글 상태(모집공고, 지원서, 회비)와 각 항목의 요약 정보를 반환합니다.
        - 모집공고: 모집 기간 정보
        - 지원서: 문항 개수
        - 회비: 금액, 은행, 계좌번호, 예금주

        요약 정보가 설정되지 않은 경우 해당 필드는 null로 반환됩니다.

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        """)
    @GetMapping("/{clubId}/settings")
    ResponseEntity<ClubSettingsResponse> getSettings(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer userId
    );

    @Operation(summary = "동아리 설정을 변경한다.", description = """
        동아리의 토글 설정(모집공고, 지원서, 회비 활성화 여부)을 변경합니다.

        요청에 포함된 필드만 업데이트됩니다. (PATCH 방식)
        - isRecruitmentEnabled: 모집공고 활성화 여부
        - isApplicationEnabled: 지원서 활성화 여부
        - isFeeEnabled: 회비 활성화 여부

        ## 에러
        - NOT_FOUND_CLUB (404): 동아리를 찾을 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        - FORBIDDEN_CLUB_MANAGER_ACCESS (403): 동아리 매니저 권한이 없습니다.
        """)
    @PatchMapping("/{clubId}/settings")
    ResponseEntity<ClubSettingsResponse> updateSettings(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSettingsUpdateRequest request,
        @UserId Integer userId
    );
}
