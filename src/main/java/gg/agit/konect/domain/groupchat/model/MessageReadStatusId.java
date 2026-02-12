package gg.agit.konect.domain.groupchat.model;

import static lombok.AccessLevel.PROTECTED;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = PROTECTED)
public class MessageReadStatusId implements Serializable {

    private Integer messageId;
    private Integer userId;

    public MessageReadStatusId(Integer messageId, Integer userId) {
        this.messageId = messageId;
        this.userId = userId;
    }
}
