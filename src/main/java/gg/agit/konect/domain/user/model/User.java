package gg.agit.konect.domain.user.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.global.model.BaseEntity;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.university.model.University;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uq_reg_email_provider",
            columnNames = {"email", "provider"}
        ),
        @jakarta.persistence.UniqueConstraint(
            name = "uq_user_university_student_number",
            columnNames = {"university_id", "student_number"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "phone_number", length = 20, unique = true)
    private String phoneNumber;

    @Column(name = "student_number", length = 20, nullable = false)
    private String studentNumber;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "is_marketing_agreement", nullable = false)
    private Boolean isMarketingAgreement;

    @Column(name = "image_url")
    private String imageUrl;

    @Builder
    private User(
        Integer id,
        University university,
        String email,
        String name,
        String phoneNumber,
        String studentNumber,
        Provider provider,
        Boolean isMarketingAgreement,
        String imageUrl
    ) {
        this.id = id;
        this.university = university;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.studentNumber = studentNumber;
        this.provider = provider;
        this.isMarketingAgreement = isMarketingAgreement;
        this.imageUrl = imageUrl;
    }

    public void updateInfo(String name, String studentNumber, String phoneNumber) {
        this.name = name;
        this.studentNumber = studentNumber;
        this.phoneNumber = phoneNumber;
    }

    public boolean hasSameStudentNumber(String studentNumber) {
        return this.studentNumber.equals(studentNumber);
    }

    public boolean hasSamePhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.equals(this.phoneNumber);
    }
}
