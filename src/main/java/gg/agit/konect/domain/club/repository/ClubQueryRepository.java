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
import com.querydsl.jpa.impl.JPAQuery;
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
        BooleanBuilder condition = createClubSearchCondition(query, isRecruiting, universityId);
        OrderSpecifier<?> sort = clubSort(isRecruiting);

        List<Club> clubs = fetchClubs(pageable, condition, sort);
        Map<Integer, List<String>> clubTagsMap = fetchClubTags(clubs);
        List<ClubSummaryInfo> content = convertToSummaryInfo(clubs, clubTagsMap);
        Long total = countClubs(condition, isRecruiting);

        return new PageImpl<>(content, pageable, total);
    }

    private JPAQuery<?> baseQuery(BooleanBuilder condition) {
        return jpaQueryFactory
            .from(club)
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(condition);
    }

    private List<Club> fetchClubs(PageRequest pageable, BooleanBuilder condition, OrderSpecifier<?> sort) {
        return baseQuery(condition)
            .select(club)
            .orderBy(sort)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    private Long countClubs(BooleanBuilder condition, Boolean isRecruiting) {
        JPAQuery<Long> query = jpaQueryFactory
            .select(club.countDistinct())
            .from(club);

        if (isRecruiting) {
            query.leftJoin(clubRecruitment).on(clubRecruitment.club.id.eq(club.id));
        }

        return query.where(condition).fetchOne();
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

    private JPAQuery<Integer> createClubIdsByTagNameSubquery(String normalizedQuery) {
        return jpaQueryFactory
            .select(clubTagMap.club.id)
            .from(clubTagMap)
            .innerJoin(clubTagMap.tag, clubTag)
            .where(clubTag.name.lower().contains(normalizedQuery));
    }

    private BooleanBuilder createClubSearchCondition(String query, Boolean isRecruiting, Integer universityId) {
        BooleanBuilder condition = new BooleanBuilder();

        addUniversityCondition(condition, universityId);
        addQuerySearchCondition(condition, query);
        addRecruitingCondition(condition, isRecruiting);

        return condition;
    }

    private void addUniversityCondition(BooleanBuilder condition, Integer universityId) {
        condition.and(club.university.id.eq(universityId));
    }

    private void addQuerySearchCondition(BooleanBuilder condition, String query) {
        if (StringUtils.isEmpty(query)) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        BooleanBuilder searchCondition = new BooleanBuilder();
        searchCondition.or(club.name.lower().contains(normalizedQuery));
        searchCondition.or(club.id.in(createClubIdsByTagNameSubquery(normalizedQuery)));

        condition.and(searchCondition);
    }

    private void addRecruitingCondition(BooleanBuilder condition, Boolean isRecruiting) {
        if (!isRecruiting) {
            return;
        }

        LocalDate today = LocalDate.now();
        condition.and(clubRecruitment.id.isNotNull())
            .and(clubRecruitment.startDate.loe(today))
            .and(clubRecruitment.endDate.goe(today));
    }

    private OrderSpecifier<?> clubSort(Boolean isRecruiting) {
        if (isRecruiting) {
            return clubRecruitment.endDate.asc();
        }

        return club.id.asc();
    }

    private List<ClubSummaryInfo> convertToSummaryInfo(List<Club> clubs, Map<Integer, List<String>> clubTagsMap) {
        return clubs.stream()
            .map(clubEntity -> {
                ClubRecruitment recruitment = clubEntity.getClubRecruitment();
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
}
