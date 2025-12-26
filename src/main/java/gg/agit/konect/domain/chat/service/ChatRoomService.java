package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_PRESIDENT;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMessagesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsResponse;
import gg.agit.konect.domain.chat.dto.CreateChatRoomRequest;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
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
    private final ClubMemberRepository clubMemberRepository;

    @Transactional
    public ChatRoomResponse createOrGetChatRoom(Integer userId, CreateChatRoomRequest request) {
        ClubMember clubPresident = clubMemberRepository.findPresidentByClubId(request.clubId())
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CLUB_PRESIDENT));

        User currentUser = userRepository.getById(userId);
        User president = clubPresident.getUser();

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(currentUser.getId(), president.getId())
            .orElseGet(() -> {
                ChatRoom newChatRoom = ChatRoom.of(currentUser, president);
                return chatRoomRepository.save(newChatRoom);
            });

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoomsResponse getChatRooms(Integer userId) {
        User user = userRepository.getById(userId);
        List<ChatRoom> chatRooms = chatRoomRepository.findByUserId(userId);
        return ChatRoomsResponse.from(chatRooms, user);
    }

    @Transactional
    public ChatMessagesResponse getChatRoomMessages(Integer userId, Integer roomId, Integer page, Integer limit) {
        ChatRoom chatRoom = chatRoomRepository.getById(roomId);
        if (chatRoom.isNotParticipant(userId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        List<ChatMessage> unreadMessages = chatMessageRepository.findUnreadMessages(roomId, userId);
        unreadMessages.forEach(ChatMessage::markAsRead);

        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, pageable);
        return ChatMessagesResponse.from(messages, userId);
    }

    @Transactional
    public ChatMessageResponse sendMessage(Integer userId, Integer roomId, ChatMessageSendRequest request) {
        ChatRoom chatRoom = chatRoomRepository.getById(roomId);
        if (chatRoom.isNotParticipant(userId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }

        User sender = userRepository.getById(userId);
        User receiver = chatRoom.getChatPartner(sender);

        ChatMessage message = ChatMessage.of(chatRoom, sender, receiver, request.content());
        ChatMessage savedMessage = chatMessageRepository.save(message);
        return ChatMessageResponse.from(savedMessage, userId);
    }
}
