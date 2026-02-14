package gg.agit.konect.domain.chat.group.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GroupChatReadStatusId implements Serializable {

    private Integer roomId;
    private Integer userId;
}
