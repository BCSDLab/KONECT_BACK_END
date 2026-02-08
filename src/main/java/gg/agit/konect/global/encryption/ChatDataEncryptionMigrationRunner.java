package gg.agit.konect.global.encryption;

import java.util.List;

import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
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
public class ChatDataEncryptionMigrationRunner implements ApplicationRunner {

    private static final int BATCH_SIZE = 100;

    private final ChatEncryptionService chatEncryptionService;
    private final EncryptionProperties encryptionProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

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
            Page<ChatMessage> messagesPage = chatMessageRepository.findAll(pageable);
            List<ChatMessage> messages = messagesPage.getContent();

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

            if (messagesPage.isLast()) {
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
            Page<ChatRoom> chatRoomsPage = chatRoomRepository.findAll(pageable);
            List<ChatRoom> chatRooms = chatRoomsPage.getContent();

            if (chatRooms.isEmpty()) {
                break;
            }

            for (ChatRoom chatRoom : chatRooms) {
                processedCount++;
                if (StringUtils.hasText(chatRoom.getLastMessageContent())
                    && isPlaintext(chatRoom.getLastMessageContent())) {
                    encryptChatRoom(chatRoom);
                    encryptedCount++;
                } else {
                    skippedCount++;
                }
            }

            if (chatRoomsPage.isLast()) {
                break;
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
            chatEncryptionService.decrypt(value, encryptionProperties.getChatKey());
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private void encryptChatMessage(ChatMessage message) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                String plaintext = message.getContent();
                String encrypted = chatEncryptionService.encrypt(plaintext, encryptionProperties.getChatKey());

                String sql = "UPDATE chat_message SET content = ? WHERE id = ?";
                jdbcTemplate.update(sql, encrypted, message.getId());

                log.debug("Message {} encrypted successfully", message.getId());
            } catch (Exception e) {
                log.error("Failed to encrypt message {}", message.getId(), e);
            }
        });
    }

    private void encryptChatRoom(ChatRoom chatRoom) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                String plaintext = chatRoom.getLastMessageContent();
                String encrypted = chatEncryptionService.encrypt(plaintext, encryptionProperties.getChatKey());

                String sql = "UPDATE chat_room SET last_message_content = ? WHERE id = ?";
                jdbcTemplate.update(sql, encrypted, chatRoom.getId());

                log.debug("ChatRoom {} encrypted successfully", chatRoom.getId());
            } catch (Exception e) {
                log.error("Failed to encrypt chat room {}", chatRoom.getId(), e);
            }
        });
    }
}
