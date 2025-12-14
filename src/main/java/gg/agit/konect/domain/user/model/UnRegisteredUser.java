package gg.agit.konect.domain.user.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.common.model.BaseEntity;
import gg.agit.konect.domain.user.enums.Provider;
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
    name = "unregistered_user",
    uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(
            name = "uq_unreg_email_provider",
            columnNames = {"email", "provider"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class UnRegisteredUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Builder
    private UnRegisteredUser(Integer id, String email, Provider provider) {
        this.id = id;
        this.email = email;
        this.provider = provider;
    }
}
