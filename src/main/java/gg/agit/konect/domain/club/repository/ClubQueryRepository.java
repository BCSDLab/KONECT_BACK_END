package gg.agit.konect.domain.club.repository;

import static gg.agit.konect.domain.club.model.QClub.club;
import static gg.agit.konect.domain.club.model.QClubRecruitment.clubRecruitment;
import static gg.agit.konect.domain.club.model.QClubTag.clubTag;
import static gg.agit.konect.domain.club.model.QClubTagMap.clubTagMap;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubSummaryInfo;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ClubQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public Page<ClubSummaryInfo> findAllByFilter(
        PageRequest pageable, String query, Boolean isRecruiting, Integer universityId
    ) {
        BooleanBuilder filter = clubSearchFilter(query, isRecruiting, universityId);
        OrderSpecifier<?> sort = clubSort(isRecruiting);

        List<Tuple> clubData = fetchClubs(pageable, filter, sort);
        List<Club> clubs = clubData.stream()
            .map(tuple -> tuple.get(club))
            .toList();
        Map<Integer, List<String>> clubTagsMap = fetchClubTags(clubs);
        List<ClubSummaryInfo> content = convertToSummaryInfo(clubData, clubTagsMap);
        Long total = countClubs(filter);

        return new PageImpl<>(content, pageable, total);
    }

    private List<Tuple> fetchClubs(PageRequest pageable, BooleanBuilder filter, OrderSpecifier<?> sort) {
        return jpaQueryFactory
            .select(club, clubRecruitment)
            .from(club)
            .leftJoin(clubRecruitment).on(clubRecruitment.club.id.eq(club.id))
            .where(filter)
            .orderBy(sort)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    private Map<Integer, List<String>> fetchClubTags(List<Club> clubs) {
        List<Integer> clubIds = clubs.stream()
            .map(Club::getId)
            .toList();

        if (clubIds.isEmpty()) {
            return Map.of();
        }

        List<Tuple> tagResults = jpaQueryFactory
            .select(clubTagMap.club.id, clubTag.name)
            .from(clubTagMap)
            .innerJoin(clubTagMap.tag, clubTag)
            .where(clubTagMap.club.id.in(clubIds))
            .fetch();

        return tagResults.stream()
            .collect(Collectors.groupingBy(
                tuple -> tuple.get(clubTagMap.club.id),
                Collectors.mapping(tuple -> tuple.get(clubTag.name), Collectors.toList())
            ));
    }

    private List<ClubSummaryInfo> convertToSummaryInfo(List<Tuple> clubData, Map<Integer, List<String>> clubTagsMap) {
        return clubData.stream()
            .map(tuple -> {
                Club clubEntity = tuple.get(club);
                ClubRecruitment recruitment = tuple.get(clubRecruitment);
                RecruitmentStatus status = RecruitmentStatus.of(recruitment);

                return new ClubSummaryInfo(
                    clubEntity.getId(),
                    clubEntity.getName(),
                    clubEntity.getImageUrl(),
                    clubEntity.getClubCategory().getDescription(),
                    clubEntity.getDescription(),
                    status,
                    clubTagsMap.getOrDefault(clubEntity.getId(), List.of())
                );
            })
            .toList();
    }

    private Long countClubs(BooleanBuilder filter) {
        return jpaQueryFactory
            .select(club.countDistinct())
            .from(club)
            .leftJoin(clubRecruitment).on(clubRecruitment.club.id.eq(club.id))
            .where(filter)
            .fetchOne();
    }

    private BooleanBuilder clubSearchFilter(String query, Boolean isRecruiting, Integer universityId) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(club.university.id.eq(universityId));

        if (!StringUtils.isEmpty(query)) {
            String normalizedQuery = query.trim().toLowerCase();

            BooleanBuilder searchBuilder = new BooleanBuilder();
            searchBuilder.or(club.name.lower().contains(normalizedQuery));
            searchBuilder.or(club.id.in(
                jpaQueryFactory
                    .select(clubTagMap.club.id)
                    .from(clubTagMap)
                    .innerJoin(clubTagMap.tag, clubTag)
                    .where(clubTag.name.lower().contains(normalizedQuery))
            ));

            builder.and(searchBuilder);
        }

        if (isRecruiting) {
            LocalDate today = LocalDate.now();
            builder.and(clubRecruitment.id.isNotNull())
                .and(clubRecruitment.startDate.loe(today))
                .and(clubRecruitment.endDate.goe(today));
        }

        return builder;
    }

    private OrderSpecifier<?> clubSort(Boolean isRecruiting) {
        if (isRecruiting) {
            return clubRecruitment.endDate.asc();
        }

        return club.id.asc();
    }
}
