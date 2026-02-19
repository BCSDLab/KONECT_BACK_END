package gg.agit.konect.domain.club.model;

import static gg.agit.konect.global.code.ApiResponseCode.INVALID_REQUEST_BODY;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

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

    @Column(name = "fee_amount", length = 100)
    private String feeAmount;

    @Column(name = "fee_bank", length = 100)
    private String feeBank;

    @Column(name = "fee_account_number", length = 100)
    private String feeAccountNumber;

    @Column(name = "fee_account_holder", length = 100)
    private String feeAccountHolder;

    @Column(name = "is_fee_required")
    private Boolean isFeeRequired;

    @Column(name = "is_recruitment_enabled")
    private Boolean isRecruitmentEnabled;

    @Column(name = "is_application_enabled")
    private Boolean isApplicationEnabled;

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
        String feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        Boolean isFeeRequired,
        Boolean isRecruitmentEnabled,
        Boolean isApplicationEnabled,
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
        this.isFeeRequired = isFeeRequired;
        this.isRecruitmentEnabled = isRecruitmentEnabled;
        this.isApplicationEnabled = isApplicationEnabled;
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
        String feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        if (isFeeInfoEmpty(feeAmount, feeBank, feeAccountNumber, feeAccountHolder)) {
            clearFeeInfo();
            return;
        }

        if (!isFeeInfoComplete(feeAmount, feeBank, feeAccountNumber, feeAccountHolder)) {
            throw CustomException.of(INVALID_REQUEST_BODY);
        }

        updateFeeInfo(feeAmount, feeBank, feeAccountNumber, feeAccountHolder);
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

    public void updateSettings(
        Boolean isRecruitmentEnabled,
        Boolean isApplicationEnabled,
        Boolean isFeeRequired
    ) {
        if (isRecruitmentEnabled != null) {
            this.isRecruitmentEnabled = isRecruitmentEnabled;
        }
        if (isApplicationEnabled != null) {
            this.isApplicationEnabled = isApplicationEnabled;
        }
        if (isFeeRequired != null) {
            this.isFeeRequired = isFeeRequired;
        }
    }

    private boolean isFeeInfoEmpty(
        String feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        return !StringUtils.hasText(feeAmount)
            && !StringUtils.hasText(feeBank)
            && !StringUtils.hasText(feeAccountNumber)
            && !StringUtils.hasText(feeAccountHolder);
    }

    private boolean isFeeInfoComplete(
        String feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        return StringUtils.hasText(feeAmount)
            && StringUtils.hasText(feeBank)
            && StringUtils.hasText(feeAccountNumber)
            && StringUtils.hasText(feeAccountHolder);
    }

    private void updateFeeInfo(
        String feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        this.feeAmount = feeAmount;
        this.feeBank = feeBank;
        this.feeAccountNumber = feeAccountNumber;
        this.feeAccountHolder = feeAccountHolder;
    }

    private void clearFeeInfo() {
        this.feeAmount = null;
        this.feeBank = null;
        this.feeAccountNumber = null;
        this.feeAccountHolder = null;
    }
}
