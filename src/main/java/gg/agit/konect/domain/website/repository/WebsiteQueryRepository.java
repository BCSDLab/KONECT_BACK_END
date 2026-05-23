package gg.agit.konect.domain.website.repository;

import static gg.agit.konect.domain.web.model.QWebClub.webClub;
import static gg.agit.konect.domain.web.model.QWebUniversity.webUniversity;

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
import gg.agit.konect.domain.university.enums.UniversityRegion;
import gg.agit.konect.domain.web.model.WebClub;
import gg.agit.konect.domain.web.model.WebUniversity;
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
        NumberExpression<Long> clubCount = webClub.id.countDistinct();

        List<Tuple> rows = jpaQueryFactory
            .select(
                webUniversity.id,
                webUniversity.koreanName,
                webUniversity.campus,
                webUniversity.region,
                webUniversity.imageUrl,
                clubCount
            )
            .from(webUniversity)
            .leftJoin(webClub).on(webClub.university.id.eq(webUniversity.id))
            .where(condition)
            .groupBy(
                webUniversity.id,
                webUniversity.koreanName,
                webUniversity.campus,
                webUniversity.region,
                webUniversity.imageUrl
            )
            .orderBy(webUniversity.koreanName.asc(), webUniversity.campus.asc())
            .fetch();

        return rows.stream()
            .map(row -> new WebsiteUniversitySummary(
                row.get(webUniversity.id),
                row.get(webUniversity.koreanName),
                row.get(webUniversity.campus).getDisplayName(),
                row.get(webUniversity.region),
                row.get(webUniversity.region).getDisplayName(),
                row.get(webUniversity.imageUrl),
                row.get(clubCount)
            ))
            .toList();
    }

    public Optional<WebUniversity> findUniversity(Integer universityId) {
        return Optional.ofNullable(jpaQueryFactory
            .selectFrom(webUniversity)
            .where(webUniversity.id.eq(universityId))
            .fetchOne());
    }

    public Page<WebClub> findClubs(
        Integer universityId,
        String query,
        ClubCategory category,
        PageRequest pageable
    ) {
        BooleanBuilder condition = createClubCondition(universityId, query, category);

        List<WebClub> clubs = jpaQueryFactory
            .selectFrom(webClub)
            .join(webClub.university, webUniversity).fetchJoin()
            .where(condition)
            .orderBy(webClub.name.asc(), webClub.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(webClub.count())
            .from(webClub)
            .where(condition)
            .fetchOne();

        return new PageImpl<>(clubs, pageable, total == null ? 0 : total);
    }

    public Map<ClubCategory, Long> countClubCategories(Integer universityId, String query) {
        BooleanBuilder condition = createClubCondition(universityId, query, null);
        NumberExpression<Long> clubCount = webClub.count();

        List<Tuple> rows = jpaQueryFactory
            .select(webClub.clubCategory, clubCount)
            .from(webClub)
            .where(condition)
            .groupBy(webClub.clubCategory)
            .fetch();

        Map<ClubCategory, Long> categoryCounts = new LinkedHashMap<>();
        rows.forEach(row -> categoryCounts.put(row.get(webClub.clubCategory), row.get(clubCount)));
        return categoryCounts;
    }

    public Long countClubsByUniversityId(Integer universityId) {
        Long count = jpaQueryFactory
            .select(webClub.count())
            .from(webClub)
            .where(webClub.university.id.eq(universityId))
            .fetchOne();

        return count == null ? 0 : count;
    }

    public Optional<WebClub> findClub(Integer clubId) {
        return Optional.ofNullable(jpaQueryFactory
            .selectFrom(webClub)
            .join(webClub.university, webUniversity).fetchJoin()
            .where(webClub.id.eq(clubId))
            .fetchOne());
    }

    public List<WebClub> findClubs(List<Integer> clubIds) {
        if (clubIds.isEmpty()) {
            return List.of();
        }

        return jpaQueryFactory
            .selectFrom(webClub)
            .join(webClub.university, webUniversity).fetchJoin()
            .where(webClub.id.in(clubIds))
            .fetch();
    }

    private BooleanBuilder createClubCondition(Integer universityId, String query, ClubCategory category) {
        BooleanBuilder condition = new BooleanBuilder();

        condition.and(webClub.university.id.eq(universityId));
        addClubSearchCondition(condition, query);
        if (category != null) {
            condition.and(webClub.clubCategory.eq(category));
        }

        return condition;
    }

    private void addUniversitySearchCondition(BooleanBuilder condition, String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        condition.and(webUniversity.koreanName.lower().contains(normalizedQuery));
    }

    private void addUniversityRegionCondition(BooleanBuilder condition, UniversityRegion region) {
        if (region == null) {
            return;
        }

        condition.and(webUniversity.region.eq(region));
    }

    private void addClubSearchCondition(BooleanBuilder condition, String query) {
        if (query == null || query.isBlank()) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        BooleanExpression nameContains = webClub.name.lower().contains(normalizedQuery);
        condition.and(nameContains);
    }
}
