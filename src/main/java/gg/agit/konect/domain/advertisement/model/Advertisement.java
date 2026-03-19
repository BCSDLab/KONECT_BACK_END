package gg.agit.konect.domain.advertisement.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "advertisement")
@NoArgsConstructor(access = PROTECTED)
public class Advertisement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "title", length = 100, nullable = false)
    private String title;

    @Column(name = "description", length = 255, nullable = false)
    private String description;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Column(name = "link_url", length = 255, nullable = false)
    private String linkUrl;

    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "click_count", nullable = false)
    private Integer clickCount;

    private Advertisement(
        String title,
        String description,
        String imageUrl,
        String linkUrl,
        Boolean isVisible,
        Integer clickCount
    ) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.isVisible = isVisible;
        this.clickCount = clickCount;
    }

    public static Advertisement of(
        String title,
        String description,
        String imageUrl,
        String linkUrl,
        Boolean isVisible
    ) {
        return new Advertisement(
            title,
            description,
            imageUrl,
            linkUrl,
            isVisible,
            0
        );
    }

    public void update(
        String title,
        String description,
        String imageUrl,
        String linkUrl,
        Boolean isVisible
    ) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.isVisible = isVisible;
    }

    public void increaseClickCount() {
        this.clickCount++;
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public Boolean getIsVisible() {
        return isVisible;
    }

    public Integer getClickCount() {
        return clickCount;
    }

    public LocalDateTime getCreatedAt() {
        return super.getCreatedAt();
    }

    public LocalDateTime getUpdatedAt() {
        return super.getUpdatedAt();
    }
}
