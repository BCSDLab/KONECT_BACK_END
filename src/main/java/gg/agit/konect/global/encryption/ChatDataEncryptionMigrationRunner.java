package gg.agit.konect.global.encryption;

import java.util.ArrayList;
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

        if (!StringUtils.hasText(encryptionProperties.getSecretKey())) {
            log.error("app.encryption.secret-key is not set. Migration skipped.");
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

            List<Object[]> batchUpdates = new ArrayList<>();

            for (ChatMessage message : messages) {
                processedCount++;
                if (isPlaintext(message.getContent())) {
                    try {
                        String encrypted = chatEncryptionService.encrypt(
                            message.getContent(),
                            encryptionProperties.getSecretKey()
                        );
                        batchUpdates.add(new Object[] {encrypted, message.getId()});
                        encryptedCount++;
                    } catch (Exception e) {
                        skippedCount++;
                        log.error("Failed to encrypt message {}", message.getId(), e);
                    }
                } else {
                    skippedCount++;
                }
            }

            executeMessageBatchUpdate(batchUpdates);

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

            List<Object[]> batchUpdates = new ArrayList<>();

            for (ChatRoom chatRoom : chatRooms) {
                processedCount++;
                if (StringUtils.hasText(chatRoom.getLastMessageContent())
                    && isPlaintext(chatRoom.getLastMessageContent())) {
                    try {
                        String encrypted = chatEncryptionService.encrypt(
                            chatRoom.getLastMessageContent(),
                            encryptionProperties.getSecretKey()
                        );
                        batchUpdates.add(new Object[] {encrypted, chatRoom.getId()});
                        encryptedCount++;
                    } catch (Exception e) {
                        skippedCount++;
                        log.error("Failed to encrypt chat room {}", chatRoom.getId(), e);
                    }
                } else {
                    skippedCount++;
                }
            }

            executeChatRoomBatchUpdate(batchUpdates);

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
            chatEncryptionService.decrypt(value, encryptionProperties.getSecretKey());
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private void executeMessageBatchUpdate(List<Object[]> batchUpdates) {
        if (batchUpdates.isEmpty()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
            "UPDATE chat_message SET content = ? WHERE id = ?",
            batchUpdates
        ));
    }

    private void executeChatRoomBatchUpdate(List<Object[]> batchUpdates) {
        if (batchUpdates.isEmpty()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.batchUpdate(
            "UPDATE chat_room SET last_message_content = ? WHERE id = ?",
            batchUpdates
        ));
    }
}
