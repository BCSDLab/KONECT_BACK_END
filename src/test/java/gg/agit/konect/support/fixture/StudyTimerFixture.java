package gg.agit.konect.support.fixture;

import java.time.LocalDateTime;

import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.studytime.model.StudyTimer;
import gg.agit.konect.domain.user.model.User;

public class StudyTimerFixture {

    public static StudyTimer createStartedTimer(User user, LocalDateTime startedAt) {
        StudyTimer studyTimer = StudyTimer.of(user, startedAt);
        ReflectionTestUtils.setField(studyTimer, "createdAt", startedAt);
        ReflectionTestUtils.setField(studyTimer, "updatedAt", startedAt);
        return studyTimer;
    }
}
