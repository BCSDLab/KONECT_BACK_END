package gg.agit.konect.support;

import java.io.IOException;
import java.net.ServerSocket;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import redis.embedded.RedisServer;

@TestConfiguration
public class EmbeddedRedisConfig {

    private static final int DEFAULT_REDIS_PORT = 0; // 0이면 랜덤 포트

    private int actualPort;
    private RedisServer redisServer;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", actualPort);
    }

    @PostConstruct
    public void startRedis() throws IOException {
        // 사용 가능한 랜덤 포트 찾기
        actualPort = findAvailablePort();
        redisServer = new RedisServer(actualPort);
        redisServer.start();
    }

    @PreDestroy
    public void stopRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    private int findAvailablePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
