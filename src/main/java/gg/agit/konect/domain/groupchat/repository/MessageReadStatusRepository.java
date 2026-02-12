package gg.agit.konect.domain.groupchat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.groupchat.dto.MessageReadCount;
import gg.agit.konect.domain.groupchat.model.MessageReadStatus;
import gg.agit.konect.domain.groupchat.model.MessageReadStatusId;

public interface MessageReadStatusRepository extends Repository<MessageReadStatus, MessageReadStatusId> {

    MessageReadStatus save(MessageReadStatus status);

    List<MessageReadStatus> saveAll(Iterable<MessageReadStatus> statuses);

    int countByMessageId(Integer messageId);

    List<MessageReadStatus> findByMessageIdIn(List<Integer> messageIds);

    @Query("""
        SELECT new gg.agit.konect.domain.groupchat.dto.MessageReadCount(rs.messageId, COUNT(rs))
        FROM MessageReadStatus rs
        WHERE rs.messageId IN :messageIds
        GROUP BY rs.messageId
        """)
    List<MessageReadCount> countReadCountByMessageIds(@Param("messageIds") List<Integer> messageIds);

    boolean existsByMessageIdAndUserId(Integer messageId, Integer userId);
}
