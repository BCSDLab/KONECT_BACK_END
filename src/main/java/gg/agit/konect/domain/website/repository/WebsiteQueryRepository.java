package gg.agit.konect.domain.website.repository;

import static gg.agit.konect.domain.club.model.QClub.club;
import static gg.agit.konect.domain.club.model.QClubMember.clubMember;
import static gg.agit.konect.domain.club.model.QClubRecruitment.clubRecruitment;
import static gg.agit.konect.domain.university.model.QUniversity.university;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.website.model.WebsiteUniversitySummary;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class WebsiteQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<WebsiteUniversitySummary> findUniversitySummaries(String query, UniversityRegion region) {
        BooleanBuilder condition = new BooleanBuilder();
        addUniversitySearchCondition(condition, query);
        addUniversityRegionCondition(condition, region);
        NumberExpression<Long> clubCount = club.id.countDistinct();

        List<Tuple> rows = jpaQueryFactory
            .select(
                university.id,
                university.koreanName,
                university.campus,
                university.region,
                university.logoImageUrl,
                clubCount
            )
            .from(university)
            .leftJoin(club).on(club.university.id.eq(university.id))
            .where(condition)
            .groupBy(
                university.id,
                university.koreanName,
                university.campus,
                university.region,
                university.logoImageUrl
            )
            .orderBy(university.koreanName.asc(), university.campus.asc())
            .fetch();

        return rows.stream()
            .map(row -> new WebsiteUniversitySummary(
                row.get(university.id),
                row.get(university.koreanName),
                row.get(university.campus).getDisplayName(),
                row.get(university.region),
                row.get(university.region).getDisplayName(),
                row.get(university.logoImageUrl),
                row.get(clubCount)
            ))
            .toList();
    }

    public Optional<University> findUniversity(Integer universityId) {
        return Optional.ofNullable(jpaQueryFactory
            .selectFrom(university)
            .where(university.id.eq(universityId))
            .fetchOne());
    }

    public Page<Club> findClubs(
        Integer universityId,
        String query,
        ClubCategory category,
        PageRequest pageable
    ) {
        BooleanBuilder condition = createClubCondition(universityId, query, category);

        List<Club> clubs = jpaQueryFactory
            .selectFrom(club)
            .join(club.university, university).fetchJoin()
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(condition)
            .orderBy(club.name.asc(), club.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(club.count())
            .from(club)
            .where(condition)
            .fetchOne();

        return new PageImpl<>(clubs, pageable, total == null ? 0 : total);
    }

    public Map<ClubCategory, Long> countClubCategories(Integer universityId, String query) {
        BooleanBuilder condition = createClubCondition(universityId, query, null);
        NumberExpression<Long> clubCount = club.count();

        List<Tuple> rows = jpaQueryFactory
            .select(club.clubCategory, clubCount)
            .from(club)
            .where(condition)
            .groupBy(club.clubCategory)
            .fetch();

        Map<ClubCategory, Long> categoryCounts = new LinkedHashMap<>();
        rows.forEach(row -> categoryCounts.put(row.get(club.clubCategory), row.get(clubCount)));
        return categoryCounts;
    }

    public Optional<Club> findClub(Integer clubId) {
        return Optional.ofNullable(jpaQueryFactory
            .selectFrom(club)
            .join(club.university, university).fetchJoin()
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(club.id.eq(clubId))
            .fetchOne());
    }

    public List<Club> findClubs(List<Integer> clubIds) {
        if (clubIds.isEmpty()) {
            return List.of();
        }

        return jpaQueryFactory
            .selectFrom(club)
            .join(club.university, university).fetchJoin()
            .leftJoin(club.clubRecruitment, clubRecruitment).fetchJoin()
            .where(club.id.in(clubIds))
            .fetch();
    }

    public Map<Integer, Long> countMembersByClubIds(List<Integer> clubIds) {
        if (clubIds.isEmpty()) {
            return Map.of();
        }
        NumberExpression<Long> memberCount = clubMember.count();

        List<Tuple> rows = jpaQueryFactory
            .select(clubMember.club.id, memberCount)
            .from(clubMember)
            .where(
                clubMember.club.id.in(clubIds),
                clubMember.user.deletedAt.isNull()
            )
            .groupBy(clubMember.club.id)
            .fetch();

        Map<Integer, Long> memberCounts = new LinkedHashMap<>();
        rows.forEach(row -> memberCounts.put(row.get(clubMember.club.id), row.get(memberCount)));
        return memberCounts;
    }

    private BooleanBuilder createClubCondition(Integer universityId, String query, ClubCategory category) {
        BooleanBuilder condition = new BooleanBuilder();

        condition.and(club.university.id.eq(universityId));
        addClubSearchCondition(condition, query);
        if (category != null) {
            condition.and(club.clubCategory.eq(category));
        }

        return condition;
    }

    private void addUniversitySearchCondition(BooleanBuilder condition, String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        condition.and(university.koreanName.lower().contains(normalizedQuery));
    }

    private void addUniversityRegionCondition(BooleanBuilder condition, UniversityRegion region) {
        if (region == null) {
            return;
        }

        condition.and(university.region.eq(region));
    }

    private void addClubSearchCondition(BooleanBuilder condition, String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        BooleanExpression nameContains = club.name.lower().contains(normalizedQuery);
        condition.and(nameContains);
    }
}
