package gg.agit.konect.domain.user.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_users_phone_number",
            columnNames = {"phone_number"}
        ),
        @UniqueConstraint(
            name = "uq_users_email_provider",
            columnNames = {"email", "provider"}
        ),
        @UniqueConstraint(
            name = "uq_users_university_id_student_number",
            columnNames = {"university_id", "student_number"}
        ),
        @UniqueConstraint(
            name = "uq_users_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseEntity {

    private static final Integer STUDENT_NUMBER_YEAR_MAX_LENGTH = 4;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "name", length = 5, nullable = false)
    private String name;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "student_number", length = 20, nullable = false)
    private String studentNumber;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "is_marketing_agreement", nullable = false)
    private Boolean isMarketingAgreement;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "apple_refresh_token", length = 1024)
    private String appleRefreshToken;

    @Builder
    private User(
        Integer id,
        University university,
        String email,
        String name,
        String phoneNumber,
        String studentNumber,
        Provider provider,
        String providerId,
        UserRole role,
        Boolean isMarketingAgreement,
        String imageUrl,
        String appleRefreshToken
    ) {
        this.id = id;
        this.university = university;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.studentNumber = studentNumber;
        this.provider = provider;
        this.providerId = providerId;
        this.role = role == null ? UserRole.USER : role;
        this.isMarketingAgreement = isMarketingAgreement;
        this.imageUrl = imageUrl;
        this.appleRefreshToken = appleRefreshToken;
    }

    public static User of(
        University university,
        UnRegisteredUser tempUser,
        String name,
        String studentNumber,
        Boolean isMarketingAgreement,
        String imageUrl
    ) {
        return User.builder()
            .university(university)
            .email(tempUser.getEmail())
            .name(name)
            .studentNumber(studentNumber)
            .provider(tempUser.getProvider())
            .providerId(tempUser.getProviderId())
            .isMarketingAgreement(isMarketingAgreement)
            .imageUrl(imageUrl)
            .appleRefreshToken(tempUser.getAppleRefreshToken())
            .build();
    }

    public void updateInfo(String name, String studentNumber, String phoneNumber) {
        this.name = name;
        this.studentNumber = studentNumber;
        this.phoneNumber = phoneNumber;
    }

    public void updateRepresentativeInfo(String name, String phoneNumber, String email) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public boolean hasSameStudentNumber(String studentNumber) {
        return this.studentNumber.equals(studentNumber);
    }

    public boolean hasSamePhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.equals(this.phoneNumber);
    }

    public String getStudentNumberYear() {
        return studentNumber.substring(0, STUDENT_NUMBER_YEAR_MAX_LENGTH);
    }

    public boolean isAdmin() {
        return this.role.equals(UserRole.ADMIN);
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
    }
}
