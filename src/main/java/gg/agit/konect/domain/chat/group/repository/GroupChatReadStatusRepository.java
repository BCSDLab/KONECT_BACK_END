package gg.agit.konect.domain.chat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.chat.group.model.GroupChatReadStatus;
import gg.agit.konect.domain.chat.group.model.GroupChatReadStatusId;

public interface GroupChatReadStatusRepository extends Repository<GroupChatReadStatus, GroupChatReadStatusId> {

    GroupChatReadStatus save(GroupChatReadStatus status);

    Optional<GroupChatReadStatus> findByRoomIdAndUserId(Integer roomId, Integer userId);

    List<GroupChatReadStatus> findByRoomId(Integer roomId);
}
