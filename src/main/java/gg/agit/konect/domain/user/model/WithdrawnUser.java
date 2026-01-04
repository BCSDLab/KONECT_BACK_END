package gg.agit.konect.domain.user.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "withdrawn_users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_withdrawn_users_email_provider",
            columnNames = {"email", "provider"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class WithdrawnUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "provider", length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "original_user_id", nullable = false)
    private Integer originalUserId;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    @Column(name = "university_id")
    private Integer universityId;

    @Column(name = "student_number", length = 20)
    private String studentNumber;

    public static WithdrawnUser from(User user) {
        WithdrawnUser withdrawnUser = new WithdrawnUser();
        withdrawnUser.email = user.getEmail();
        withdrawnUser.provider = user.getProvider();
        withdrawnUser.originalUserId = user.getId();
        withdrawnUser.withdrawnAt = LocalDateTime.now();
        withdrawnUser.universityId = user.getUniversity().getId();
        withdrawnUser.studentNumber = user.getStudentNumber();
        return withdrawnUser;
    }
}
