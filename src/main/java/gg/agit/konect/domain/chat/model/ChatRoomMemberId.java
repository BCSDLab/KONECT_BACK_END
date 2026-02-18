package gg.agit.konect.domain.chat.model;

import java.io.Serial;
import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMemberId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "chat_room_id", nullable = false, updatable = false)
    private Integer chatRoomId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Integer userId;
}
