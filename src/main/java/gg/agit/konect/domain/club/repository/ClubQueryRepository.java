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
        BooleanBuilder filter = clubSearchFilter(query, isRecruiting, universityId);
        OrderSpecifier<?> sort = clubSort(isRecruiting);

        List<Club> clubs = fetchClubs(pageable, filter, sort);
        Map<Integer, List<String>> clubTagsMap = fetchClubTags(clubs);
        List<ClubSummaryInfo> content = convertToSummaryInfo(clubs, clubTagsMap);
        Long total = countClubs(filter, isRecruiting);

        return new PageImpl<>(content, pageable, total);
    }

    private JPAQuery<?> baseQuery(BooleanBuilder filter) {
        return jpaQueryFactory
            .from(club)
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(filter);
    }

    private List<Club> fetchClubs(PageRequest pageable, BooleanBuilder filter, OrderSpecifier<?> sort) {
        return baseQuery(filter)
            .select(club)
            .orderBy(sort)
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
    }

    private Long countClubs(BooleanBuilder filter, Boolean isRecruiting) {
        JPAQuery<Long> query = jpaQueryFactory
            .select(club.countDistinct())
            .from(club);

        if (isRecruiting) {
            query.leftJoin(clubRecruitment).on(clubRecruitment.club.id.eq(club.id));
        }

        return query.where(filter).fetchOne();
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
