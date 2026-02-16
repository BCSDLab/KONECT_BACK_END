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

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_bank", length = 100)
    private String feeBank;

    @Column(name = "fee_account_number", length = 100)
    private String feeAccountNumber;

    @Column(name = "fee_account_holder", length = 100)
    private String feeAccountHolder;

    @Column(name = "is_fee_required", columnDefinition = "TINYINT(1)")
    private Boolean isFeeRequired;

    @OneToOne(mappedBy = "club", fetch = LAZY, cascade = ALL, orphanRemoval = true)
    private ClubRecruitment clubRecruitment;

    /**
     * Constructs a Club instance populated with all its properties.
     *
     * @param feeAmount         the fee amount for the club or {@code null} if not set
     * @param feeBank           the bank name for fee payments or {@code null} if not set
     * @param feeAccountNumber  the account number for fee payments or {@code null} if not set
     * @param feeAccountHolder  the account holder name for fee payments or {@code null} if not set
     * @param isFeeRequired     {@code true} if the club requires a fee, {@code false} if it does not, or {@code null} if unspecified
     * @param clubRecruitment   the associated ClubRecruitment entity or {@code null} if none
     */
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
        Boolean isFeeRequired,
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

    /**
     * Replaces the club's fee information according to the provided fee fields.
     *
     * If all fee fields are null, existing fee information is cleared. If some but not all
     * required fee fields are provided, a validation exception is thrown. Otherwise the
     * fee fields are updated together with the `isFeeRequired` flag.
     *
     * @param feeAmount         the fee amount in smallest currency unit, or {@code null} to omit
     * @param feeBank           the bank name for fee payment, or {@code null} to omit
     * @param feeAccountNumber  the bank account number for fee payment, or {@code null} to omit
     * @param feeAccountHolder  the account holder name for fee payment, or {@code null} to omit
     * @param isFeeRequired     whether a fee is required for the club, or {@code null} to omit
     * @throws CustomException  with ApiResponseCode.INVALID_REQUEST_BODY when fee fields are partially provided
     */
    public void replaceFeeInfo(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        Boolean isFeeRequired
    ) {
        if (isFeeInfoEmpty(feeAmount, feeBank, feeAccountNumber, feeAccountHolder)) {
            clearFeeInfo();
            return;
        }

        if (!isFeeInfoComplete(feeAmount, feeBank, feeAccountNumber, feeAccountHolder)) {
            throw CustomException.of(INVALID_REQUEST_BODY);
        }

        updateFeeInfo(feeAmount, feeBank, feeAccountNumber, feeAccountHolder, isFeeRequired);
    }

    /**
     * Updates the club's description, image URL, location, and introduction.
     *
     * @param description brief description of the club
     * @param imageUrl URL of the club's image
     * @param location meeting place or address for the club
     * @param introduce detailed introduction text for the club
     */
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

    /**
     * Determines whether all fee-related inputs are absent.
     *
     * @param feeAmount the fee amount value to check
     * @param feeBank the fee recipient bank name to check
     * @param feeAccountNumber the fee recipient account number to check
     * @param feeAccountHolder the fee recipient account holder name to check
     * @return `true` if all four fee fields are `null`, `false` otherwise
     */
    private boolean isFeeInfoEmpty(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        return feeAmount == null
            && feeBank == null
            && feeAccountNumber == null
            && feeAccountHolder == null;
    }

    /**
     * Determines whether all fee-related fields are provided.
     *
     * @param feeAmount        the fee amount (may be null)
     * @param feeBank          the bank name for the fee account
     * @param feeAccountNumber the account number for the fee
     * @param feeAccountHolder the account holder name for the fee
     * @return                 `true` if `feeAmount` is non-null and all string parameters contain non-whitespace text, `false` otherwise
     */
    private boolean isFeeInfoComplete(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder
    ) {
        return feeAmount != null
            && StringUtils.hasText(feeBank)
            && StringUtils.hasText(feeAccountNumber)
            && StringUtils.hasText(feeAccountHolder);
    }

    /**
     * Set the club's fee details.
     *
     * @param feeAmount        the fee amount in the smallest currency unit (nullable)
     * @param feeBank          the name of the bank for fee payment (nullable)
     * @param feeAccountNumber the account number for fee payment (nullable)
     * @param feeAccountHolder the account holder name for fee payment (nullable)
     * @param isFeeRequired    `true` if the club requires a fee, `false` if not, or `null` if unspecified
     */
    private void updateFeeInfo(
        Integer feeAmount,
        String feeBank,
        String feeAccountNumber,
        String feeAccountHolder,
        Boolean isFeeRequired
    ) {
        this.feeAmount = feeAmount;
        this.feeBank = feeBank;
        this.feeAccountNumber = feeAccountNumber;
        this.feeAccountHolder = feeAccountHolder;
        this.isFeeRequired = isFeeRequired;
    }

    /**
     * Clears all stored fee-related information from this club.
     *
     * Sets feeAmount, feeBank, feeAccountNumber, feeAccountHolder, and isFeeRequired to null.
     */
    private void clearFeeInfo() {
        this.feeAmount = null;
        this.feeBank = null;
        this.feeAccountNumber = null;
        this.feeAccountHolder = null;
        this.isFeeRequired = null;
    }
}