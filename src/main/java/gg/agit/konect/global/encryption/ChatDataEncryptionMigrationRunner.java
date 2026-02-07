package gg.agit.konect.global.encryption;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.encryption.migration-enabled", havingValue = "true")
public class ChatDataEncryptionMigrationRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 100;

    private final EncryptionUtil encryptionUtil;
    private final EncryptionProperties encryptionProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) throws Exception {
        log.info("Start chat data encryption migration");

        if (!StringUtils.hasText(encryptionProperties.getChatKey())) {
            log.error("APP_CHAT_ENCRYPTION_KEY is not set. Migration skipped.");
            return;
        }

        migrateChatMessages();
        migrateChatRooms();

        log.info("Chat data encryption migration completed");
    }

    private void migrateChatMessages() {
        log.info("Starting ChatMessage migration");

        int page = 0;
        int processedCount = 0;
        int encryptedCount = 0;
        int skippedCount = 0;

        while (true) {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(0, pageable)
                .getContent();

            if (messages.isEmpty()) {
                break;
            }

            for (ChatMessage message : messages) {
                processedCount++;
                if (isPlaintext(message.getContent())) {
                    encryptChatMessage(message);
                    encryptedCount++;
                } else {
                    skippedCount++;
                }
            }

            if (messages.size() < BATCH_SIZE) {
                break;
            }

            page++;
        }

        log.info("ChatMessage migration completed - processed: {}, encrypted: {}, skipped: {}",
            processedCount, encryptedCount, skippedCount);
    }

    private void migrateChatRooms() {
        log.info("Starting ChatRoom migration");

        int processedCount = 0;
        int encryptedCount = 0;
        int skippedCount = 0;

        int page = 0;
        while (true) {
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            List<ChatRoom> chatRooms = chatRoomRepository.findAllAdminChatRooms(null);

            if (chatRooms.isEmpty()) {
                break;
            }

            int start = page * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, chatRooms.size());

            if (start >= chatRooms.size()) {
                break;
            }

            List<ChatRoom> batch = chatRooms.subList(start, end);

            for (ChatRoom chatRoom : batch) {
                processedCount++;
                if (StringUtils.hasText(chatRoom.getLastMessageContent())
                    && isPlaintext(chatRoom.getLastMessageContent())) {
                    encryptChatRoom(chatRoom);
                    encryptedCount++;
                } else {
                    skippedCount++;
                }
            }

            page++;
        }

        log.info("ChatRoom migration completed - processed: {}, encrypted: {}, skipped: {}",
            processedCount, encryptedCount, skippedCount);
    }

    private boolean isPlaintext(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            encryptionUtil.decrypt(value, encryptionProperties.getChatKey());
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    @Transactional
    private void encryptChatMessage(ChatMessage message) {
        try {
            String plaintext = message.getContent();
            String encrypted = encryptionUtil.encrypt(plaintext, encryptionProperties.getChatKey());

            String sql = "UPDATE chat_message SET content = ? WHERE id = ?";
            jdbcTemplate.update(sql, encrypted, message.getId());

            log.debug("Message {} encrypted successfully", message.getId());
        } catch (Exception e) {
            log.error("Failed to encrypt message {}", message.getId(), e);
        }
    }

    @Transactional
    private void encryptChatRoom(ChatRoom chatRoom) {
        try {
            String plaintext = chatRoom.getLastMessageContent();
            String encrypted = encryptionUtil.encrypt(plaintext, encryptionProperties.getChatKey());

            String sql = "UPDATE chat_room SET last_message_content = ? WHERE id = ?";
            jdbcTemplate.update(sql, encrypted, chatRoom.getId());

            log.debug("ChatRoom {} encrypted successfully", chatRoom.getId());
        } catch (Exception e) {
            log.error("Failed to encrypt chat room {}", chatRoom.getId(), e);
        }
    }
}
