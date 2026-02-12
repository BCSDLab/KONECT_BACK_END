package gg.agit.konect.domain.groupchat.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.groupchat.model.GroupChatRoom;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface GroupChatRoomRepository extends Repository<GroupChatRoom, Integer> {

    GroupChatRoom save(GroupChatRoom room);

    Optional<GroupChatRoom> findById(Integer roomId);

    Optional<GroupChatRoom> findByClubId(Integer clubId);

    Optional<Integer> findIdByClubId(Integer clubId);

    default GroupChatRoom getById(Integer roomId) {
        return findById(roomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }

    default GroupChatRoom getByClubId(Integer clubId) {
        return findByClubId(clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }

    default Integer getIdByClubId(Integer clubId) {
        return findIdByClubId(clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }
}
