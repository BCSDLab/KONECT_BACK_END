package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubApply;
import gg.agit.konect.domain.user.model.User;

public class ClubApplyFixture {

    public static ClubApply create(Club club, User user) {
        return ClubApply.of(club, user, null);
    }

    public static ClubApply createWithFeePayment(Club club, User user, String feePaymentImageUrl) {
        return ClubApply.of(club, user, feePaymentImageUrl);
    }
}
