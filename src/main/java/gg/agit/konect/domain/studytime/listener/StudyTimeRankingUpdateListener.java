package gg.agit.konect.domain.studytime.listener;

import static org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import gg.agit.konect.domain.studytime.event.StudyTimeAccumulatedEvent;
import gg.agit.konect.domain.studytime.service.StudyTimeRankingUpdateService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StudyTimeRankingUpdateListener {

    private final StudyTimeRankingUpdateService studyTimeRankingUpdateService;

    @TransactionalEventListener(phase = AFTER_COMMIT)
    @Transactional
    public void handleStudyTimeAccumulated(StudyTimeAccumulatedEvent event) {
        studyTimeRankingUpdateService.updateRankingsForUser(event.userId());
    }
}
