package gg.agit.konect.domain.groupchat.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.groupchat.model.GroupChatMessage;

public interface GroupChatMessageRepository extends Repository<GroupChatMessage, Integer> {

    GroupChatMessage save(GroupChatMessage message);

    Page<GroupChatMessage> findByRoomIdAndIdGreaterThanOrderByCreatedAtDesc(
        Integer roomId,
        Integer minMessageId,
        Pageable pageable
    );

    Optional<GroupChatMessage> findTopByRoomIdOrderByCreatedAtDesc(Integer roomId);
}
