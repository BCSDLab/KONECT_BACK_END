package gg.agit.konect.unit.domain.event.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.event.dto.EventBoothMapResponse;
import gg.agit.konect.domain.event.dto.EventContentsResponse;
import gg.agit.konect.domain.event.dto.EventHomeResponse;
import gg.agit.konect.domain.event.dto.EventMiniEventsResponse;
import gg.agit.konect.domain.event.enums.EventBoothMapItemStatus;
import gg.agit.konect.domain.event.enums.EventContentType;
import gg.agit.konect.domain.event.enums.EventProgressStatus;
import gg.agit.konect.domain.event.model.Event;
import gg.agit.konect.domain.event.model.EventBooth;
import gg.agit.konect.domain.event.model.EventBoothMap;
import gg.agit.konect.domain.event.model.EventBoothMapItem;
import gg.agit.konect.domain.event.model.EventContent;
import gg.agit.konect.domain.event.model.EventMiniEvent;
import gg.agit.konect.domain.event.repository.EventBoothMapItemRepository;
import gg.agit.konect.domain.event.repository.EventBoothMapRepository;
import gg.agit.konect.domain.event.repository.EventBoothRepository;
import gg.agit.konect.domain.event.repository.EventContentRepository;
import gg.agit.konect.domain.event.repository.EventMiniEventRepository;
import gg.agit.konect.domain.event.repository.EventProgramRepository;
import gg.agit.konect.domain.event.repository.EventRepository;
import gg.agit.konect.domain.event.service.EventService;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;

class EventServiceTest extends ServiceTestSupport {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventProgramRepository eventProgramRepository;

    @Mock
    private EventBoothRepository eventBoothRepository;

    @Mock
    private EventBoothMapRepository eventBoothMapRepository;

    @Mock
    private EventBoothMapItemRepository eventBoothMapItemRepository;

    @Mock
    private EventMiniEventRepository eventMiniEventRepository;

    @Mock
    private EventContentRepository eventContentRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    @DisplayName("getEventHome은 행사 요약 카운트를 응답에 담는다")
    void getEventHomeReturnsSummaryCounts() {
        // given
        Event event = createEvent(1, "대동제", "봄 축제", "https://poster", "공지");
        given(eventRepository.findById(1)).willReturn(Optional.of(event));
        given(eventProgramRepository.countByEventId(1)).willReturn(3);
        given(eventBoothRepository.countByEventId(1)).willReturn(5);
        given(eventMiniEventRepository.countByEventId(1)).willReturn(2);
        given(eventContentRepository.countByEventId(1)).willReturn(4);

        // when
        EventHomeResponse response = eventService.getEventHome(1, 10);

        // then
        assertThat(response.eventId()).isEqualTo(1);
        assertThat(response.title()).isEqualTo("대동제");
        assertThat(response.summary().programCount()).isEqualTo(3);
        assertThat(response.summary().boothCount()).isEqualTo(5);
        assertThat(response.summary().eventCount()).isEqualTo(2);
        assertThat(response.summary().contentCount()).isEqualTo(4);
        assertThat(response.userStatus().point()).isZero();
        assertThat(response.userStatus().participatedEventCount()).isZero();
    }

    @Test
    @DisplayName("getEventBoothMap은 맵과 부스 좌표 정보를 응답으로 변환한다")
    void getEventBoothMapReturnsMappedBoothsAndZones() {
        // given
        Event event = createEvent(1, "대동제", "봄 축제", "https://poster", "공지");
        EventBoothMap boothMap = createBoothMap(11, event, "https://map", 1200, 800);
        EventBooth scienceBooth = createBooth(101, event, "AI 부스", "체험", "A-1", "ZONE-A", true);
        EventBooth artBooth = createBooth(102, event, "전시 부스", "전시", "B-2", "ZONE-B", false);
        EventBoothMapItem scienceItem = createBoothMapItem(1001, boothMap, scienceBooth, 10, 20, 30, 40,
            EventBoothMapItemStatus.OPEN);
        EventBoothMapItem artItem = createBoothMapItem(1002, boothMap, artBooth, 50, 60, 70, 80,
            EventBoothMapItemStatus.CLOSED);

        given(eventBoothMapRepository.findByEventId(1)).willReturn(Optional.of(boothMap));
        given(eventBoothMapItemRepository.findAllByEventBoothMapIdOrderByIdAsc(11))
            .willReturn(List.of(scienceItem, artItem));

        // when
        EventBoothMapResponse response = eventService.getEventBoothMap(1);

        // then
        assertThat(response.mapImageUrl()).isEqualTo("https://map");
        assertThat(response.zones())
            .extracting(EventBoothMapResponse.ZoneResponse::code)
            .containsExactly("ZONE-A", "ZONE-B");
        assertThat(response.booths())
            .extracting(
                EventBoothMapResponse.BoothMapItemResponse::boothId,
                EventBoothMapResponse.BoothMapItemResponse::name,
                EventBoothMapResponse.BoothMapItemResponse::zone,
                EventBoothMapResponse.BoothMapItemResponse::status
            )
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(101, "AI 부스", "ZONE-A", "OPEN"),
                org.assertj.core.groups.Tuple.tuple(102, "전시 부스", "ZONE-B", "CLOSED")
            );
    }

    @Test
    @DisplayName("getEventMiniEvents는 페이지 범위만 잘라 응답한다")
    void getEventMiniEventsReturnsPagedItems() {
        // given
        Event event = createEvent(1, "대동제", "봄 축제", "https://poster", "공지");
        EventMiniEvent first = createMiniEvent(201, event, "스탬프 투어", "캠퍼스를 돌며 참여", "10P",
            EventProgressStatus.ONGOING);
        EventMiniEvent second = createMiniEvent(202, event, "룰렛", "즉석 경품 이벤트", "굿즈",
            EventProgressStatus.UPCOMING);
        EventMiniEvent third = createMiniEvent(203, event, "퀴즈", "상식 퀴즈", "5P",
            EventProgressStatus.ENDED);

        given(eventRepository.findById(1)).willReturn(Optional.of(event));
        given(eventMiniEventRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(1))
            .willReturn(List.of(first, second, third));

        // when
        EventMiniEventsResponse response = eventService.getEventMiniEvents(1, 2, 2, 10);

        // then
        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.currentCount()).isEqualTo(1);
        assertThat(response.totalPage()).isEqualTo(2);
        assertThat(response.currentPage()).isEqualTo(2);
        assertThat(response.miniEvents())
            .extracting(item -> item.miniEventId(), item -> item.title(), item -> item.status())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(203, "퀴즈", "ENDED"));
    }

    @Test
    @DisplayName("getEventContents는 category와 페이지를 함께 반영한다")
    void getEventContentsFiltersByCategoryAndPaginates() {
        // given
        Event event = createEvent(1, "대동제", "봄 축제", "https://poster", "공지");
        EventContent article = createContent(301, event, "기사", "기사 요약", EventContentType.ARTICLE,
            LocalDateTime.of(2026, 4, 12, 9, 0));
        EventContent image = createContent(302, event, "포토", "사진 요약", EventContentType.IMAGE,
            LocalDateTime.of(2026, 4, 12, 10, 0));
        EventContent video = createContent(303, event, "영상", "영상 요약", EventContentType.VIDEO,
            LocalDateTime.of(2026, 4, 12, 11, 0));

        given(eventRepository.findById(1)).willReturn(Optional.of(event));
        given(eventContentRepository.findAllByEventIdOrderByDisplayOrderAscIdAsc(1))
            .willReturn(List.of(article, image, video));

        // when
        EventContentsResponse response = eventService.getEventContents(1, "image", 1, 1);

        // then
        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.currentCount()).isEqualTo(1);
        assertThat(response.totalPage()).isEqualTo(1);
        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.contents())
            .extracting(item -> item.contentId(), item -> item.title(), item -> item.type())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(302, "포토", "IMAGE"));
    }

    @Test
    @DisplayName("getEventHome은 행사가 없으면 NOT_FOUND_EVENT를 던진다")
    void getEventHomeThrowsWhenEventMissing() {
        // given
        given(eventRepository.findById(1)).willReturn(Optional.empty());

        // when & then
        assertErrorCode(() -> eventService.getEventHome(1, 10), NOT_FOUND_EVENT);
    }

    private Event createEvent(Integer id, String title, String subtitle, String posterImageUrl, String notice) {
        Event event = instantiate(Event.class);
        ReflectionTestUtils.setField(event, "id", id);
        ReflectionTestUtils.setField(event, "title", title);
        ReflectionTestUtils.setField(event, "subtitle", subtitle);
        ReflectionTestUtils.setField(event, "posterImageUrl", posterImageUrl);
        ReflectionTestUtils.setField(event, "notice", notice);
        ReflectionTestUtils.setField(event, "startAt", LocalDateTime.of(2026, 4, 12, 10, 0));
        ReflectionTestUtils.setField(event, "endAt", LocalDateTime.of(2026, 4, 12, 22, 0));
        ReflectionTestUtils.setField(event, "status", "PUBLISHED");
        return event;
    }

    private EventBoothMap createBoothMap(Integer id, Event event, String mapImageUrl, Integer width, Integer height) {
        EventBoothMap boothMap = instantiate(EventBoothMap.class);
        ReflectionTestUtils.setField(boothMap, "id", id);
        ReflectionTestUtils.setField(boothMap, "event", event);
        ReflectionTestUtils.setField(boothMap, "mapImageUrl", mapImageUrl);
        ReflectionTestUtils.setField(boothMap, "width", width);
        ReflectionTestUtils.setField(boothMap, "height", height);
        return boothMap;
    }

    private EventBooth createBooth(Integer id, Event event, String name, String category, String locationLabel, String zone,
        Boolean isOpen) {
        EventBooth booth = instantiate(EventBooth.class);
        ReflectionTestUtils.setField(booth, "id", id);
        ReflectionTestUtils.setField(booth, "event", event);
        ReflectionTestUtils.setField(booth, "name", name);
        ReflectionTestUtils.setField(booth, "category", category);
        ReflectionTestUtils.setField(booth, "locationLabel", locationLabel);
        ReflectionTestUtils.setField(booth, "zone", zone);
        ReflectionTestUtils.setField(booth, "thumbnailUrl", "https://thumb/" + id);
        ReflectionTestUtils.setField(booth, "isOpen", isOpen);
        ReflectionTestUtils.setField(booth, "displayOrder", 1);
        return booth;
    }

    private EventBoothMapItem createBoothMapItem(Integer id, EventBoothMap boothMap, EventBooth booth, Integer x, Integer y,
        Integer width, Integer height, EventBoothMapItemStatus status) {
        EventBoothMapItem boothMapItem = instantiate(EventBoothMapItem.class);
        ReflectionTestUtils.setField(boothMapItem, "id", id);
        ReflectionTestUtils.setField(boothMapItem, "eventBoothMap", boothMap);
        ReflectionTestUtils.setField(boothMapItem, "eventBooth", booth);
        ReflectionTestUtils.setField(boothMapItem, "x", x);
        ReflectionTestUtils.setField(boothMapItem, "y", y);
        ReflectionTestUtils.setField(boothMapItem, "width", width);
        ReflectionTestUtils.setField(boothMapItem, "height", height);
        ReflectionTestUtils.setField(boothMapItem, "status", status);
        return boothMapItem;
    }

    private EventMiniEvent createMiniEvent(Integer id, Event event, String title, String description, String rewardLabel,
        EventProgressStatus status) {
        EventMiniEvent miniEvent = instantiate(EventMiniEvent.class);
        ReflectionTestUtils.setField(miniEvent, "id", id);
        ReflectionTestUtils.setField(miniEvent, "event", event);
        ReflectionTestUtils.setField(miniEvent, "title", title);
        ReflectionTestUtils.setField(miniEvent, "description", description);
        ReflectionTestUtils.setField(miniEvent, "thumbnailUrl", "https://mini/" + id);
        ReflectionTestUtils.setField(miniEvent, "rewardLabel", rewardLabel);
        ReflectionTestUtils.setField(miniEvent, "status", status);
        ReflectionTestUtils.setField(miniEvent, "displayOrder", 1);
        return miniEvent;
    }

    private EventContent createContent(Integer id, Event event, String title, String summary, EventContentType type,
        LocalDateTime publishedAt) {
        EventContent content = instantiate(EventContent.class);
        ReflectionTestUtils.setField(content, "id", id);
        ReflectionTestUtils.setField(content, "event", event);
        ReflectionTestUtils.setField(content, "title", title);
        ReflectionTestUtils.setField(content, "summary", summary);
        ReflectionTestUtils.setField(content, "thumbnailUrl", "https://content/" + id);
        ReflectionTestUtils.setField(content, "type", type);
        ReflectionTestUtils.setField(content, "publishedAt", publishedAt);
        ReflectionTestUtils.setField(content, "displayOrder", 1);
        return content;
    }

    private void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable,
        gg.agit.konect.global.code.ApiResponseCode expectedCode) {
        org.assertj.core.api.Assertions.assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode()).isEqualTo(expectedCode));
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception exception) {
            throw new IllegalStateException(type.getSimpleName() + " test fixture 생성에 실패했습니다.", exception);
        }
    }
}
