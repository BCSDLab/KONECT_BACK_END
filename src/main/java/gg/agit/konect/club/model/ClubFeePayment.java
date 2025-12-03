package gg.agit.konect.club.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDate;

import gg.agit.konect.club.enums.FeePaymentStatus;
import gg.agit.konect.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "club_fee_payment")
@NoArgsConstructor(access = PROTECTED)
public class ClubFeePayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "date", nullable = false, updatable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(value = STRING)
    @Column(name = "status", nullable = false)
    private FeePaymentStatus status;

    @Column(name = "exempt_reason")
    private String exemptReason;

    @ManyToOne(fetch = LAZY)
    @JoinColumns({
        @JoinColumn(name = "club_id", nullable = false),
        @JoinColumn(name = "user_id", nullable = false)
    })
    private ClubMember clubMember;

    @Builder
    private ClubFeePayment(Integer id, LocalDate date, FeePaymentStatus status, String exemptReason,
        ClubMember clubMember) {
        this.id = id;
        this.date = date;
        this.status = status;
        this.exemptReason = exemptReason;
        this.clubMember = clubMember;
    }

    public boolean isUnPaid() {
        return status == FeePaymentStatus.UNPAID;
    }
}
