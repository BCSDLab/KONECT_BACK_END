package gg.agit.konect.domain.user.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import gg.agit.konect.domain.university.model.University;
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
import jakarta.persistence.OneToMany;
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
        @UniqueConstraint(name = "uq_users_phone_number_active",
            columnNames = {"phone_number", "active_flag"}
        ),
        @UniqueConstraint(
            name = "uq_users_university_id_student_number_active",
            columnNames = {"university_id", "student_number", "active_flag"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseEntity {

    private static final Integer STUDENT_NUMBER_YEAR_MAX_LENGTH = 4;
    private static final String WITHDRAWN_USER_NAME = "탈퇴한 사용자";

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "name", length = 30, nullable = false)
    private String name;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "student_number", length = 20, nullable = false)
    private String studentNumber;

    @Column(name = "role", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "is_marketing_agreement", nullable = false)
    private Boolean isMarketingAgreement;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "user", fetch = LAZY)
    private final List<UserOAuthAccount> oauthAccounts = new ArrayList<>();

    @Column(name = "last_login_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_activity_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime lastActivityAt;

    @Column(name = "deleted_at", columnDefinition = "TIMESTAMP")
    private LocalDateTime deletedAt;

    @Column(name = "active_flag", insertable = false, updatable = false)
    private Integer activeFlag;

    @Builder
    private User(
        Integer id,
        University university,
        String email,
        String name,
        String phoneNumber,
        String studentNumber,
        UserRole role,
        Boolean isMarketingAgreement,
        String imageUrl
    ) {
        this.id = id;
        this.university = university;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.studentNumber = studentNumber;
        this.role = role == null ? UserRole.USER : role;
        this.isMarketingAgreement = isMarketingAgreement;
        this.imageUrl = imageUrl;
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
            .isMarketingAgreement(isMarketingAgreement)
            .imageUrl(imageUrl)
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

    public String getName() {
        if (deletedAt != null) {
            return WITHDRAWN_USER_NAME;
        }
        return name;
    }

    public boolean isAdmin() {
        return this.role.equals(UserRole.ADMIN);
    }

    public void updateLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
        this.lastActivityAt = lastLoginAt;
    }

    public void updateLastActivityAt(LocalDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public void withdraw(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean canRestore(LocalDateTime now, long restoreWindowDays) {
        return deletedAt != null && deletedAt.isAfter(now.minusDays(restoreWindowDays));
    }
}
