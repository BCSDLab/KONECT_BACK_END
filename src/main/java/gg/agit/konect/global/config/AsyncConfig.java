package gg.agit.konect.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.aop.interceptor.SimpleAsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final int DEFAULT_CORE_POOL_SIZE = 2;
    private static final int DEFAULT_MAX_POOL_SIZE = 5;
    private static final int DEFAULT_QUEUE_CAPACITY = 50;
    private static final int DEFAULT_AWAIT_TERMINATION_SECONDS = 30;

    private static final int SHEET_SYNC_CORE_POOL_SIZE = 2;
    private static final int SHEET_SYNC_MAX_POOL_SIZE = 4;
    private static final int SHEET_SYNC_QUEUE_CAPACITY = 50;
    private static final int SHEET_SYNC_AWAIT_TERMINATION_SECONDS = 30;

    private static final int NOTIFICATION_CORE_POOL_SIZE = 2;
    private static final int NOTIFICATION_MAX_POOL_SIZE = 5;
    private static final int NOTIFICATION_QUEUE_CAPACITY = 100;
    private static final int NOTIFICATION_AWAIT_TERMINATION_SECONDS = 30;

    private static final int SLACK_CORE_POOL_SIZE = 1;
    private static final int SLACK_MAX_POOL_SIZE = 3;
    private static final int SLACK_QUEUE_CAPACITY = 50;
    private static final int SLACK_AWAIT_TERMINATION_SECONDS = 30;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(DEFAULT_CORE_POOL_SIZE);
        executor.setMaxPoolSize(DEFAULT_MAX_POOL_SIZE);
        executor.setQueueCapacity(DEFAULT_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("async-default-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(DEFAULT_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new SimpleAsyncUncaughtExceptionHandler();
    }

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

    @Bean(name = "notificationTaskExecutor")
    public Executor notificationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(NOTIFICATION_CORE_POOL_SIZE);
        executor.setMaxPoolSize(NOTIFICATION_MAX_POOL_SIZE);
        executor.setQueueCapacity(NOTIFICATION_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("notification-");
        executor.setRejectedExecutionHandler((runnable, pool) -> {
            log.warn("알림 스레드풀 포화로 작업이 거절되었습니다. poolSize={}, activeCount={}, queueSize={}",
                pool.getPoolSize(), pool.getActiveCount(), pool.getQueue().size());
            throw new RejectedExecutionException("notificationTaskExecutor saturated");
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(NOTIFICATION_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }

    @Bean(name = "slackTaskExecutor")
    public Executor slackTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(SLACK_CORE_POOL_SIZE);
        executor.setMaxPoolSize(SLACK_MAX_POOL_SIZE);
        executor.setQueueCapacity(SLACK_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("slack-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SLACK_AWAIT_TERMINATION_SECONDS);
        executor.initialize();
        return executor;
    }
}
