package gg.agit.konect.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class AsyncConfig {

    private static final int SHEET_SYNC_CORE_POOL_SIZE = 2;
    private static final int SHEET_SYNC_MAX_POOL_SIZE = 4;
    private static final int SHEET_SYNC_QUEUE_CAPACITY = 50;
    private static final int SHEET_SYNC_AWAIT_TERMINATION_SECONDS = 30;

    @Bean(name = "sheetSyncTaskExecutor")
    public Executor sheetSyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(SHEET_SYNC_CORE_POOL_SIZE);
        executor.setMaxPoolSize(SHEET_SYNC_MAX_POOL_SIZE);
        executor.setQueueCapacity(SHEET_SYNC_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("sheet-sync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SHEET_SYNC_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
