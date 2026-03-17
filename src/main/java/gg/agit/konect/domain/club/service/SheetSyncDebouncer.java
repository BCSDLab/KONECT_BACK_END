package gg.agit.konect.domain.club.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

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
        ScheduledFuture<?> existing = pendingTasks.get(clubId);
        if (existing != null && !existing.isDone()) {
            existing.cancel(false);
            log.debug("Sheet sync debounced. clubId={}", clubId);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingTasks.remove(clubId);
            sheetSyncExecutor.execute(clubId);
        }, DEBOUNCE_DELAY_SECONDS, TimeUnit.SECONDS);

        pendingTasks.put(clubId, future);
    }
}
