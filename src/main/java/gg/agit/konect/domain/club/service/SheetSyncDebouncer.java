package gg.agit.konect.domain.club.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import gg.agit.konect.domain.club.enums.ClubSheetSortKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SheetSyncDebouncer {

    private static final long DEBOUNCE_DELAY_SECONDS = 3;

    private final ConcurrentHashMap<Integer, ScheduledFuture<?>> pendingTasks =
        new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();

    private final SheetSyncExecutor sheetSyncExecutor;

    public void debounce(Integer clubId) {
        pendingTasks.compute(clubId, (id, existing) -> {
            if (existing != null && !existing.isDone()) {
                existing.cancel(false);
                log.debug("Sheet sync debounced. clubId={}", id);
            }
            return scheduler.schedule(() -> {
                pendingTasks.remove(id);
                sheetSyncExecutor.executeWithSort(id, ClubSheetSortKey.POSITION, true);
            }, DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);
        });
    }
}
