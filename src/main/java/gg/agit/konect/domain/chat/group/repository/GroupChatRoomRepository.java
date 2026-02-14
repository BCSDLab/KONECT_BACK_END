package gg.agit.konect.domain.chat.group.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.chat.group.model.GroupChatRoom;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface GroupChatRoomRepository extends Repository<GroupChatRoom, Integer> {

    GroupChatRoom save(GroupChatRoom room);

    Optional<GroupChatRoom> findById(Integer roomId);

    @Query("""
        SELECT r
        FROM GroupChatRoom r
        JOIN FETCH r.club c
        JOIN ClubMember cm ON cm.club.id = c.id
        WHERE cm.user.id = :userId
        ORDER BY r.updatedAt DESC
        """)
    List<GroupChatRoom> findAllByUserId(@Param("userId") Integer userId);

    Optional<GroupChatRoom> findByClubId(Integer clubId);

    default GroupChatRoom getById(Integer roomId) {
        return findById(roomId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }

    default GroupChatRoom getByClubId(Integer clubId) {
        return findByClubId(clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_GROUP_CHAT_ROOM));
    }

}
