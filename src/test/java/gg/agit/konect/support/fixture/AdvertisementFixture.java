package gg.agit.konect.support.fixture;

import gg.agit.konect.domain.advertisement.model.Advertisement;

public class AdvertisementFixture {

    public static Advertisement create(String title, boolean isVisible) {
        return Advertisement.of(
                title,
                title + " 설명",
                "https://example.com/advertisement.png",
                "https://example.com",
                isVisible
        );
    }
}
