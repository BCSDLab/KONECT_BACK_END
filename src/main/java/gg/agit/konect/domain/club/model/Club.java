package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_REQUEST_BODY;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import org.springframework.util.StringUtils;

import gg.agit.konect.domain.club.dto.ClubCreateRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.global.exception.CustomException;
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

    @Column(name = "description", length = 20, nullable = false)
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

    public static Club of(ClubCreateRequest request, University university) {
        return Club.builder()
            .name(request.name())
            .description(request.description())
            .introduce(request.introduce())
            .imageUrl(request.imageUrl())
            .location(request.location())
            .clubCategory(request.clubCategory())
            .university(university)
            .build();
    }

    public void replaceFeeInfo(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        LocalDate feeDeadline
    ) {
        if (isFeeInfoEmpty(feeAmount, feeBank, feeAccountNumber, feeAccountHolder, feeDeadline)) {
            clearFeeInfo();
            return;
        }

        if (!isFeeInfoComplete(feeAmount, feeBank, feeAccountNumber, feeAccountHolder, feeDeadline)) {
            throw CustomException.of(INVALID_REQUEST_BODY);
        }

        updateFeeInfo(feeAmount, feeBank, feeAccountNumber, feeAccountHolder, feeDeadline);
    }

    public void updateInfo(String description, String imageUrl, String location, String introduce) {
        this.description = description;
        this.imageUrl = imageUrl;
        this.location = location;
        this.introduce = introduce;
    }

    public void updateBasicInfo(String name, ClubCategory clubCategory) { // 어드민 계정으로만 사용 가능
        this.name = name;
        this.clubCategory = clubCategory;
    }

    private boolean isFeeInfoEmpty(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        LocalDate feeDeadline
    ) {
        return feeAmount == null
            && feeBank == null
            && feeAccountNumber == null
            && feeAccountHolder == null
            && feeDeadline == null;
    }

    private boolean isFeeInfoComplete(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        LocalDate feeDeadline
    ) {
        return feeAmount != null
            && StringUtils.hasText(feeBank)
            && StringUtils.hasText(feeAccountNumber)
            && StringUtils.hasText(feeAccountHolder)
            && feeDeadline != null;
    }

    private void updateFeeInfo(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        LocalDate feeDeadline
    ) {
        this.feeAmount = feeAmount;
        this.feeBank = feeBank;
        this.feeAccountNumber = feeAccountNumber;
        this.feeAccountHolder = feeAccountHolder;
        this.feeDeadline = feeDeadline;
    }

    private void clearFeeInfo() {
        this.feeAmount = null;
        this.feeBank = null;
        this.feeAccountNumber = null;
        this.feeAccountHolder = null;
        this.feeDeadline = null;
    }
}
