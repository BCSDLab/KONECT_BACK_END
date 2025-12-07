package gg.agit.konect.user.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.security.enums.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
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
            name = "uq_unregistered_email_provider",
            columnNames = {"email", "provider"}
        )
    }
    )
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Column(name = "phone_number", length = 20, nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "student_number", length = 20, nullable = false, unique = true)
    private String studentNumber;

    @Column(name = "is_registered", nullable = false)
    private Boolean isRegistered = Boolean.FALSE;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Builder
    private User(
        Integer id,
        String email,
        String name,
        String phoneNumber,
        String studentNumber,
        Provider provider
    ) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.studentNumber = studentNumber;
        this.provider = provider;
    }
}
