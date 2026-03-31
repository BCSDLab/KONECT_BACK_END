package gg.agit.konect.domain.chat.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_room_member")
@NoArgsConstructor(access = PROTECTED)
public class ChatRoomMember extends BaseEntity {

    @EmbeddedId
    private ChatRoomMemberId id;

    @ManyToOne(fetch = LAZY)
    @MapsId("chatRoomId")
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Column(name = "visible_message_from")
    private LocalDateTime visibleMessageFrom;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @Column(name = "custom_room_name", length = 30)
    private String customRoomName;

    @Builder
    private ChatRoomMember(
        ChatRoomMemberId id,
        ChatRoom chatRoom,
        User user,
        LocalDateTime lastReadAt,
        LocalDateTime visibleMessageFrom,
        LocalDateTime leftAt,
        String customRoomName
    ) {
        this.id = id;
        this.chatRoom = chatRoom;
        this.user = user;
        this.lastReadAt = lastReadAt;
        this.visibleMessageFrom = visibleMessageFrom;
        this.leftAt = leftAt;
        this.customRoomName = customRoomName;
    }

    public static ChatRoomMember of(ChatRoom chatRoom, User user, LocalDateTime lastReadAt) {
        return ChatRoomMember.builder()
            .id(new ChatRoomMemberId(chatRoom.getId(), user.getId()))
            .chatRoom(chatRoom)
            .user(user)
            .lastReadAt(lastReadAt)
            .build();
    }

    public Integer getChatRoomId() {
        return id.getChatRoomId();
    }

    public Integer getUserId() {
        return id.getUserId();
    }

    public void updateLastReadAt(LocalDateTime lastReadAt) {
        if (lastReadAt == null) {
            return;
        }

        if (this.lastReadAt == null || this.lastReadAt.isBefore(lastReadAt)) {
            this.lastReadAt = lastReadAt;
        }
    }

    public void updateCustomRoomName(String customRoomName) {
        this.customRoomName = customRoomName;
    }

    public boolean hasLeft() {
        return leftAt != null;
    }

    public void leaveDirectRoom(LocalDateTime leftAt) {
        this.leftAt = leftAt;
        this.visibleMessageFrom = leftAt;
        updateLastReadAt(leftAt);
    }

    /**
     * 탈퇴 이후 새 메시지가 생겨 다시 볼 수 있을 때 사용한다.
     * <p>
     * 나간 상태만 해제하고, 기존 {@code visibleMessageFrom}은 유지한다.
     * 그래서 탈퇴 이후 도착한 메시지부터 계속 보인다.
     */
    public void restoreDirectRoom() {
        this.leftAt = null;
    }

    /**
     * 사용자가 채팅방을 다시 열어 새 대화를 시작할 때 사용한다.
     * <p>
     * 나간 상태를 해제하고 {@code visibleMessageFrom}도 새로 갱신한다.
     * 그래서 전달한 시점 이후 메시지부터 새 대화처럼 보인다.
     */
    public void reopenDirectRoom(LocalDateTime visibleMessageFrom) {
        this.leftAt = null;
        this.visibleMessageFrom = visibleMessageFrom;
        updateLastReadAt(visibleMessageFrom);
    }

    public boolean hasVisibleMessages(ChatRoom room) {
        if (room.getLastMessageSentAt() == null) {
            return false;
        }

        if (visibleMessageFrom == null) {
            return true;
        }

        return room.getLastMessageSentAt().isAfter(visibleMessageFrom);
    }
}
