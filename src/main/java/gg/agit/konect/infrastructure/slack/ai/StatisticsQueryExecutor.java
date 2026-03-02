package gg.agit.konect.infrastructure.slack.ai;

import org.springframework.stereotype.Component;

import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatisticsQueryExecutor {

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final ClubRecruitmentRepository clubRecruitmentRepository;
    private final ClubMemberRepository clubMemberRepository;

    public String execute(String queryType) {
        log.debug("통계 쿼리 실행: queryType={}", queryType);

        return switch (queryType) {
            case "USER_COUNT" -> executeUserCount();
            case "CLUB_COUNT" -> executeClubCount();
            case "CLUB_RECRUITING_COUNT" -> executeClubRecruitingCount();
            case "CLUB_MEMBER_TOTAL_COUNT" -> executeClubMemberTotalCount();
            default -> null;
        };
    }

    private String executeUserCount() {
        long count = userRepository.countActiveUsers();
        return String.format("현재 가입된 활성 사용자 수: %d명", count);
    }

    private String executeClubCount() {
        long count = clubRepository.countAll();
        return String.format("전체 동아리 수: %d개", count);
    }

    private String executeClubRecruitingCount() {
        long count = clubRecruitmentRepository.countCurrentlyRecruiting();
        return String.format("현재 모집 중인 동아리 수: %d개", count);
    }

    private String executeClubMemberTotalCount() {
        long count = clubMemberRepository.countAll();
        return String.format("전체 동아리원 수: %d명", count);
    }
}
