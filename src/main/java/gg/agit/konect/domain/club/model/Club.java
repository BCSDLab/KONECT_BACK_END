package gg.agit.konect.domain.club.model;

import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "club")
@NoArgsConstructor(access = PROTECTED)
public class Club extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "club_category", nullable = false)
    private ClubCategory clubCategory;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "description", length = 100, nullable = false)
    private String description;

    @Column(name = "introduce", columnDefinition = "TEXT", nullable = false)
    private String introduce;

    @Column(name = "image_url", length = 255, nullable = false)
    private String imageUrl;

    @Column(name = "location", length = 255, nullable = false)
    private String location;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_bank", length = 100)
    private String feeBank;

    @Column(name = "fee_account_number", length = 100)
    private String feeAccountNumber;

    @Column(name = "fee_account_holder", length = 100)
    private String feeAccountHolder;

    @Column(name = "fee_deadline")
    private LocalDate feeDeadline;

    @OneToOne(mappedBy = "club", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private ClubRecruitment clubRecruitment;

    @Builder
    private Club(
        Integer id,
        ClubCategory clubCategory,
        University university,
        String name,
        String description,
        String introduce,
        String imageUrl,
        String location,
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        LocalDate feeDeadline,
        ClubRecruitment clubRecruitment
    ) {
        this.id = id;
        this.clubCategory = clubCategory;
        this.university = university;
        this.name = name;
        this.description = description;
        this.introduce = introduce;
        this.imageUrl = imageUrl;
        this.location = location;
        this.feeAmount = feeAmount;
        this.feeBank = feeBank;
        this.feeAccountNumber = feeAccountNumber;
        this.feeAccountHolder = feeAccountHolder;
        this.feeDeadline = feeDeadline;
        this.clubRecruitment = clubRecruitment;
    }
}
