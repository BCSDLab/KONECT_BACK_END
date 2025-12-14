package gg.agit.konect.domain.notice.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.domain.council.model.Council;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "council_notice")
@NoArgsConstructor(access = PROTECTED)
public class CouncilNotice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "title", nullable = false)
    private String title;

    @NotNull
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "council_id", nullable = false)
    private Council council;

    @Builder
    private CouncilNotice(Integer id, String title, String content, Council council) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.council = council;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
