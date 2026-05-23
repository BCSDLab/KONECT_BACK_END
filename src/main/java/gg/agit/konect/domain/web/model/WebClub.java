package gg.agit.konect.domain.web.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
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
@Table(name = "web_club")
@NoArgsConstructor(access = PROTECTED)
public class WebClub extends BaseEntity {

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
    private WebUniversity university;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "topic", length = 20, nullable = false)
    private String topic;

    @Column(name = "description", length = 30, nullable = false)
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

    @Column(name = "google_sheet_id", length = 255)
    private String googleSheetId;

    @Column(name = "sheet_column_mapping", columnDefinition = "JSON")
    private String sheetColumnMapping;

    @Column(name = "drive_folder_id", length = 255)
    private String driveFolderId;

    @Column(name = "template_spreadsheet_id", length = 255)
    private String templateSpreadsheetId;

    @Builder
    private WebClub(
        Integer id,
        ClubCategory clubCategory,
        WebUniversity university,
        String name,
        String topic,
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
        String googleSheetId,
        String sheetColumnMapping,
        String driveFolderId,
        String templateSpreadsheetId
    ) {
        this.id = id;
        this.clubCategory = clubCategory;
        this.university = university;
        this.name = name;
        this.topic = topic;
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
        this.googleSheetId = googleSheetId;
        this.sheetColumnMapping = sheetColumnMapping;
        this.driveFolderId = driveFolderId;
        this.templateSpreadsheetId = templateSpreadsheetId;
    }
}
