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
@Table(name = "club_information_update_request_image")
@NoArgsConstructor(access = PROTECTED)
public class ClubInformationUpdateRequestImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ClubInformationUpdateRequestEntity request;

    @NotNull
    @Column(name = "image_url", length = 500, nullable = false)
    private String imageUrl;

    @NotNull
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Builder
    private ClubInformationUpdateRequestImage(
        Integer id,
        ClubInformationUpdateRequestEntity request,
        String imageUrl,
        Integer displayOrder
    ) {
        this.id = id;
        this.request = request;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
    }

    public static class ClubInformationUpdateRequestImageBuilder {

        @Override
        public String toString() {
            return "ClubInformationUpdateRequestImage.ClubInformationUpdateRequestImageBuilder("
                + "id=" + id
                + ", imageUrl=" + imageUrl
                + ", displayOrder=" + displayOrder
                + ")";
        }
    }
}
