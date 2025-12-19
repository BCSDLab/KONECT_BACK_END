package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatRoomsResponse getChatRooms(Integer userId) {
        User user = userRepository.getById(userId);
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);
        return ChatRoomsResponse.from(chatRooms, user);
    }

    public ChatMessagesResponse getChatRoomMessages(Integer userId, Integer roomId) {
        ChatRoom chatRoom = chatRoomRepository.getById(roomId);
        if (!chatRoom.isParticipant(userId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId);
        return ChatMessagesResponse.from(messages, userId);
    }
}
