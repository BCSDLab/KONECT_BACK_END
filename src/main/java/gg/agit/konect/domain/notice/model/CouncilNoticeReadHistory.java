package gg.agit.konect.domain.notice.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "council_notice_read_history",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_council_notice_read_history_user_id_council_notice_id",
            columnNames = {"user_id, council_notice_id"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class CouncilNoticeReadHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "council_notice_id", nullable = false)
    private CouncilNotice councilNotice;

    @Builder
    private CouncilNoticeReadHistory(Integer id, User user, CouncilNotice councilNotice) {
        this.id = id;
        this.user = user;
        this.councilNotice = councilNotice;
    }

    public static CouncilNoticeReadHistory of(User user, CouncilNotice councilNotice) {
        return CouncilNoticeReadHistory.builder()
            .user(user)
            .councilNotice(councilNotice)
            .build();
    }
}
