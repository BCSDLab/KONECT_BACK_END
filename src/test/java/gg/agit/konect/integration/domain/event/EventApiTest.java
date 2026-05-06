package gg.agit.konect.integration.domain.event;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;

class EventApiTest extends IntegrationTestSupport {

    @Nested
    @DisplayName("GET /events/{eventId}/home - 행사 홈 조회")
    class GetEventHome {

        @Test
        @DisplayName("행사 기본 정보와 요약 카운트를 반환한다")
        void getEventHomeSuccess() throws Exception {
            // given
            insertEvent(1, "대동제", "봄 축제", "https://poster", "공지사항", LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 3, 22, 0));
            insertEventProgram(11, 1, "POINT", "스탬프", 1);
            insertEventProgram(12, 1, "RESONANCE", "공명", 2);
            insertEventBooth(21, 1, "AI 부스", "체험", "A-1", "ZONE-A", true, 1);
            insertEventMiniEvent(31, 1, "룰렛", "경품", "굿즈", "ONGOING", 1);
            insertEventContent(41, 1, "기사", "요약", "ARTICLE", LocalDateTime.of(2026, 5, 1, 9, 0), 1);
            clearPersistenceContext();
            mockLoginUser(10);

            // when & then
            performGet("/events/1/home")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(1))
                .andExpect(jsonPath("$.title").value("대동제"))
                .andExpect(jsonPath("$.summary.programCount").value(2))
                .andExpect(jsonPath("$.summary.boothCount").value(1))
                .andExpect(jsonPath("$.summary.eventCount").value(1))
                .andExpect(jsonPath("$.summary.contentCount").value(1));
        }

        @Test
        @DisplayName("행사가 없으면 404를 반환한다")
        void getEventHomeWhenMissing() throws Exception {
            mockLoginUser(10);

            performGet("/events/999/home")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_EVENT.getCode()));
        }
    }

    @Nested
    @DisplayName("GET /events/{eventId}/booth-map - 행사 부스 맵 조회")
    class GetEventBoothMap {

        @Test
        @DisplayName("맵 이미지와 부스 좌표 정보를 반환한다")
        void getEventBoothMapSuccess() throws Exception {
            // given
            insertEvent(1, "대동제", "봄 축제", "https://poster", "공지사항", LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 3, 22, 0));
            insertEventBooth(21, 1, "AI 부스", "체험", "A-1", "ZONE-A", true, 1);
            insertEventBoothMap(51, 1, "https://map", 1200, 800);
            insertEventBoothMapItem(61, 51, 21, 10, 20, 30, 40, "OPEN");
            clearPersistenceContext();

            // when & then
            performGet("/events/1/booth-map")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mapImageUrl").value("https://map"))
                .andExpect(jsonPath("$.zones[0].code").value("ZONE-A"))
                .andExpect(jsonPath("$.booths[0].boothId").value(21))
                .andExpect(jsonPath("$.booths[0].status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("GET /events/{eventId}/contents - 행사 콘텐츠 목록 조회")
    class GetEventContents {

        @Test
        @DisplayName("카테고리 필터와 페이지 정보를 함께 반영한다")
        void getEventContentsSuccess() throws Exception {
            // given
            insertEvent(1, "대동제", "봄 축제", "https://poster", "공지사항", LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 3, 22, 0));
            insertEventContent(41, 1, "기사", "요약1", "ARTICLE", LocalDateTime.of(2026, 5, 1, 9, 0), 1);
            insertEventContent(42, 1, "포토", "요약2", "IMAGE", LocalDateTime.of(2026, 5, 1, 10, 0), 2);
            insertEventContent(43, 1, "추가 포토", "요약3", "IMAGE", LocalDateTime.of(2026, 5, 1, 11, 0), 3);
            clearPersistenceContext();

            // when & then
            performGet("/events/1/contents?category=IMAGE&page=1&limit=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.currentCount").value(1))
                .andExpect(jsonPath("$.totalPage").value(2))
                .andExpect(jsonPath("$.contents[0].title").value("포토"))
                .andExpect(jsonPath("$.contents[0].type").value("IMAGE"));
        }
    }

    private void insertEvent(Integer id, String title, String subtitle, String posterImageUrl, String notice,
        LocalDateTime startAt, LocalDateTime endAt) {
        entityManager.createNativeQuery("""
                insert into event (id, title, subtitle, poster_image_url, notice, start_at, end_at, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)
            .setParameter(1, id)
            .setParameter(2, title)
            .setParameter(3, subtitle)
            .setParameter(4, posterImageUrl)
            .setParameter(5, notice)
            .setParameter(6, startAt)
            .setParameter(7, endAt)
            .setParameter(8, "PUBLISHED")
            .setParameter(9, startAt)
            .setParameter(10, startAt)
            .executeUpdate();
    }

    private void insertEventProgram(Integer id, Integer eventId, String type, String title, Integer displayOrder) {
        entityManager.createNativeQuery("""
                insert into event_program (id, event_id, type, title, description, thumbnail_url, reward_point, status, display_order, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventId)
            .setParameter(3, type)
            .setParameter(4, title)
            .setParameter(5, title + " 설명")
            .setParameter(6, "https://program/" + id)
            .setParameter(7, 10)
            .setParameter(8, "ONGOING")
            .setParameter(9, displayOrder)
            .executeUpdate();
    }

    private void insertEventBooth(Integer id, Integer eventId, String name, String category, String locationLabel,
        String zone,
        boolean isOpen, Integer displayOrder) {
        entityManager.createNativeQuery("""
                insert into event_booth (id, event_id, name, category, description, location_label, zone, thumbnail_url, is_open, display_order, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventId)
            .setParameter(3, name)
            .setParameter(4, category)
            .setParameter(5, name + " 설명")
            .setParameter(6, locationLabel)
            .setParameter(7, zone)
            .setParameter(8, "https://booth/" + id)
            .setParameter(9, isOpen)
            .setParameter(10, displayOrder)
            .executeUpdate();
    }

    private void insertEventBoothMap(Integer id, Integer eventId, String mapImageUrl, Integer width, Integer height) {
        entityManager.createNativeQuery("""
                insert into event_booth_map (id, event_id, map_image_url, width, height, created_at, updated_at)
                values (?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventId)
            .setParameter(3, mapImageUrl)
            .setParameter(4, width)
            .setParameter(5, height)
            .executeUpdate();
    }

    private void insertEventBoothMapItem(Integer id, Integer eventBoothMapId, Integer eventBoothId, Integer x,
        Integer y,
        Integer width, Integer height, String status) {
        entityManager.createNativeQuery("""
                insert into event_booth_map_item (id, event_booth_map_id, event_booth_id, x, y, width, height, status, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventBoothMapId)
            .setParameter(3, eventBoothId)
            .setParameter(4, x)
            .setParameter(5, y)
            .setParameter(6, width)
            .setParameter(7, height)
            .setParameter(8, status)
            .executeUpdate();
    }

    private void insertEventMiniEvent(Integer id, Integer eventId, String title, String description, String rewardLabel,
        String status, Integer displayOrder) {
        entityManager.createNativeQuery("""
                insert into event_mini_event (id, event_id, title, description, thumbnail_url, reward_label, status, display_order, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventId)
            .setParameter(3, title)
            .setParameter(4, description)
            .setParameter(5, "https://mini/" + id)
            .setParameter(6, rewardLabel)
            .setParameter(7, status)
            .setParameter(8, displayOrder)
            .executeUpdate();
    }

    private void insertEventContent(Integer id, Integer eventId, String title, String summary, String type,
        LocalDateTime publishedAt, Integer displayOrder) {
        entityManager.createNativeQuery("""
                insert into event_content (id, event_id, title, summary, body, thumbnail_url, type, published_at, display_order, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), now())
                """)
            .setParameter(1, id)
            .setParameter(2, eventId)
            .setParameter(3, title)
            .setParameter(4, summary)
            .setParameter(5, title + " 본문")
            .setParameter(6, "https://content/" + id)
            .setParameter(7, type)
            .setParameter(8, publishedAt)
            .setParameter(9, displayOrder)
            .executeUpdate();
    }
}
