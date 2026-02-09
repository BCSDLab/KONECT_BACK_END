package gg.agit.konect.domain.schedule.repository;

import static gg.agit.konect.domain.schedule.model.QSchedule.schedule;
import static gg.agit.konect.domain.schedule.model.QUniversitySchedule.universitySchedule;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import gg.agit.konect.domain.schedule.model.Schedule;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScheduleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;

    public List<Schedule> findSchedulesByMonthAndQuery(
        Integer universityId,
        LocalDateTime monthStart,
        LocalDateTime monthEnd,
        String query
    ) {
        BooleanBuilder condition = createSearchCondition(universityId, monthStart, monthEnd, query);

        return jpaQueryFactory
            .selectFrom(schedule)
            .innerJoin(universitySchedule).on(schedule.id.eq(universitySchedule.id))
            .where(condition)
            .orderBy(schedule.startedAt.asc())
            .fetch();
    }

    private BooleanBuilder createSearchCondition(
        Integer universityId,
        LocalDateTime monthStart,
        LocalDateTime monthEnd,
        String query
    ) {
        BooleanBuilder condition = new BooleanBuilder();

        addUniversityCondition(condition, universityId);
        addMonthRangeCondition(condition, monthStart, monthEnd);
        addQuerySearchCondition(condition, query);

        return condition;
    }

    private void addUniversityCondition(BooleanBuilder condition, Integer universityId) {
        condition.and(universitySchedule.university.id.eq(universityId));
    }

    private void addMonthRangeCondition(
        BooleanBuilder condition,
        LocalDateTime monthStart,
        LocalDateTime monthEnd
    ) {
        condition.and(
            schedule.startedAt.lt(monthEnd)
                .and(schedule.endedAt.gt(monthStart))
        );
    }

    private void addQuerySearchCondition(BooleanBuilder condition, String query) {
        if (StringUtils.isEmpty(query)) {
            return;
        }

        String normalizedQuery = query.trim().toLowerCase();
        condition.and(schedule.title.lower().contains(normalizedQuery));
    }
}
