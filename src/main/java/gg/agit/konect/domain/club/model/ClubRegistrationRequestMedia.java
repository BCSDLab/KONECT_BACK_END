package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
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

@Getter
@Entity
@Table(name = "club_registration_request_media")
@NoArgsConstructor(access = PROTECTED)
public class ClubRegistrationRequestMedia extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "url", length = 255, nullable = false)
    private String url;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_registration_request_id", nullable = false)
    private ClubRegistrationRequestEntity clubRegistrationRequest;

    @Builder
    private ClubRegistrationRequestMedia(
        Integer id,
        String url,
        Integer displayOrder,
        ClubRegistrationRequestEntity clubRegistrationRequest
    ) {
        this.id = id;
        this.url = url;
        this.displayOrder = displayOrder;
        this.clubRegistrationRequest = clubRegistrationRequest;
    }

    public static ClubRegistrationRequestMedia of(
        String url,
        Integer displayOrder,
        ClubRegistrationRequestEntity clubRegistrationRequest
    ) {
        return ClubRegistrationRequestMedia.builder()
            .url(url)
            .displayOrder(displayOrder)
            .clubRegistrationRequest(clubRegistrationRequest)
            .build();
    }
}
