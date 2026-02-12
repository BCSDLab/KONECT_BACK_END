package gg.agit.konect.domain.groupchat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.groupchat.model.GroupChatReadStatus;
import gg.agit.konect.domain.groupchat.model.GroupChatReadStatusId;

public interface GroupChatReadStatusRepository extends Repository<GroupChatReadStatus, GroupChatReadStatusId> {

    GroupChatReadStatus save(GroupChatReadStatus status);

    Optional<GroupChatReadStatus> findByRoomIdAndUserId(Integer roomId, Integer userId);

    List<GroupChatReadStatus> findByRoomId(Integer roomId);
}
