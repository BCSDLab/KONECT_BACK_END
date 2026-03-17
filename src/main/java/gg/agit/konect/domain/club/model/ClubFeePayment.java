package gg.agit.konect.domain.club.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "club_fee_payment")
@NoArgsConstructor(access = PROTECTED)
public class ClubFeePayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "club_id", nullable = false, updatable = false)
    private Club club;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "is_paid", nullable = false)
    private boolean isPaid;

    @Column(name = "payment_image_url")
    private String paymentImageUrl;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Builder
    private ClubFeePayment(Club club, User user, String paymentImageUrl) {
        this.club = club;
        this.user = user;
        this.isPaid = false;
        this.paymentImageUrl = paymentImageUrl;
    }

    public static ClubFeePayment of(Club club, User user, String paymentImageUrl) {
        return ClubFeePayment.builder()
            .club(club)
            .user(user)
            .paymentImageUrl(paymentImageUrl)
            .build();
    }

    public void approve(User approver) {
        this.isPaid = true;
        this.approvedAt = LocalDateTime.now();
        this.approvedBy = approver;
    }
}
