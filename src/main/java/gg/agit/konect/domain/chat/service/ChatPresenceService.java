package gg.agit.konect.domain.chat.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatPresenceService {

    private static final String PRESENCE_PREFIX = "chat:presence:room:";
    private static final String USER_SUFFIX = ":user:";
    private static final Duration PRESENCE_TTL = Duration.ofSeconds(5);

    private final StringRedisTemplate redis;

    /**
     * 사용자의 채팅방 접속 상태를 Redis에 기록합니다.
     * 5초의 TTL이 설정되어 있으며, 주기적으로 갱신되어야 합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     */
    public void recordPresence(Integer roomId, Integer userId) {
        if (roomId == null || userId == null) {
            log.warn("recordPresence: roomId or userId is null");
            return;
        }

        String key = presenceKey(roomId, userId);
        redis.opsForValue().set(key, "1", PRESENCE_TTL);
        log.debug("Recorded presence: roomId={}, userId={}", roomId, userId);
    }

    /**
     * 사용자가 특정 채팅방에 접속된 상태인지 확인합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return 사용자가 채팅방에 접속된 상태이면 true, 아니면 false
     */
    public boolean isUserInChatRoom(Integer roomId, Integer userId) {
        if (roomId == null || userId == null) {
            return false;
        }

        String key = presenceKey(roomId, userId);
        return redis.opsForValue().get(key) != null;
    }

    /**
     * Redis 키를 생성합니다.
     *
     * @param roomId 채팅방 ID
     * @param userId 사용자 ID
     * @return Redis 키 (형식: chat:presence:room:{roomId}:user:{userId})
     */
    private String presenceKey(Integer roomId, Integer userId) {
        return PRESENCE_PREFIX + roomId + USER_SUFFIX + userId;
    }
}
