package gg.agit.konect.domain.chat.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import gg.agit.konect.domain.chat.dto.ChatInvitableUsersResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.dto.ChatMessageSendRequest;
import gg.agit.konect.domain.chat.dto.ChatMuteResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomNameUpdateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomsSummaryResponse;
import gg.agit.konect.domain.chat.dto.ChatSearchResponse;
import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Tag(name = "(Normal) Chat: 채팅", description = "채팅 API")
@RequestMapping("/chats")
public interface ChatApi {

    int MAX_SEARCH_LIMIT = 100;

    @Operation(summary = "채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        ## 설명
        - 특정 유저와의 1:1 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        
        ## 로직
        - 해당 유저와의 채팅방이 이미 존재하면 기존 채팅방을 반환합니다.
        - 존재하지 않으면 새로운 채팅방을 생성합니다.
        - 자기 자신과는 채팅방을 만들 수 없습니다.
        
        ## 에러
        - CANNOT_CREATE_CHAT_ROOM_WITH_SELF (400): 자기 자신과는 채팅방을 만들 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PostMapping("/rooms")
    ResponseEntity<ChatRoomResponse> createOrGetChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "어드민과의 채팅방을 생성하거나 기존 채팅방을 반환한다.", description = """
        ## 설명
        - 문의하기 버튼에서 즉시 어드민과의 1:1 채팅으로 이동할 때 사용합니다.
        
        ## 로직
        - 시스템의 기준 어드민 계정을 찾아 해당 계정과의 채팅방을 생성하거나 기존 채팅방을 반환합니다.
        
        ## 에러
        - NOT_FOUND_USER (404): 어드민 계정을 찾을 수 없습니다.
        - CANNOT_CREATE_CHAT_ROOM_WITH_SELF (400): 자기 자신과는 채팅방을 만들 수 없습니다.
        """)
    @PostMapping("/rooms/admin")
    ResponseEntity<ChatRoomResponse> createOrGetAdminChatRoom(
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 리스트를 조회한다.", description = """
        ## 설명
        - 현재 사용자가 참여 중인 모든 채팅방 목록을 조회합니다.
        
        ## 로직
        - 각 채팅방의 상대방 정보, 마지막 메시지, 읽지 않은 메시지 수를 포함합니다.
        - 채팅방이 존재하지만 메시지 이력이 없는 경우 lastMessage, lastSentTime은 null입니다.
        - 최근 메시지가 있는 순서대로 정렬됩니다.
        """)
    @GetMapping("/rooms")
    ResponseEntity<ChatRoomsSummaryResponse> getChatRooms(
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 이름과 메시지 내용으로 채팅방을 검색한다.", description = """
        ## 설명
        - 현재 사용자가 접근 가능한 채팅방만 검색합니다.
        - 채팅방 이름 매칭 결과와 메시지 내용 매칭 결과를 분리해서 반환합니다.
        
        ## 로직
        - 1:1 채팅은 상대방 이름과 사용자가 지정한 채팅방 이름으로 검색합니다.
        - 그룹 채팅은 동아리 이름과 사용자가 지정한 채팅방 이름으로 검색합니다.
        - 메시지 검색 결과는 채팅방별 최신 매칭 메시지 1개만 반환합니다.
        - page, limit는 채팅방 이름 검색 결과와 메시지 검색 결과에 각각 동일하게 적용됩니다.
        - limit는 최대 100까지 허용됩니다.
        """)
    @GetMapping("/rooms/search")
    ResponseEntity<ChatSearchResponse> searchChats(
        @NotBlank(message = "검색어는 필수입니다.")
        @RequestParam(name = "keyword") String keyword,
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @Max(value = MAX_SEARCH_LIMIT, message = "페이지 당 항목 수는 100 이하여야 합니다.")
        @RequestParam(name = "limit", defaultValue = "20") Integer limit,
        @UserId Integer userId
    );

    @Operation(summary = "새 채팅방에 초대할 수 있는 사용자 목록을 조회한다.", description = """
        ## 설명
        - 현재 사용자가 속해 있는 채팅방들의 멤버를 기반으로 초대 가능 사용자 목록을 조회합니다.
        - 자기 자신, 탈퇴 사용자, 채팅방을 떠난 사용자는 제외됩니다.
        - 관리자 계정은 초대 대상에서 제외됩니다.
        - `sortBy=CLUB`이면 동아리 섹션별로 그룹핑되어 응답합니다.
        - `sortBy=NAME`이면 동아리 섹션 없이 이름순 단일 리스트로 응답합니다.
        - 검색어(query)는 이름과 학번에 대해 부분 일치로 동작합니다.
        """)
    @GetMapping("/rooms/invitables")
    ResponseEntity<ChatInvitableUsersResponse> getInvitableUsers(
        @RequestParam(name = "query", required = false) String query,
        @RequestParam(name = "sortBy", defaultValue = "CLUB") ChatInviteSortBy sortBy,
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 메시지 리스트를 조회한다.", description = """
        ## 설명
        - 특정 채팅방의 메시지 목록을 페이지네이션으로 조회합니다.
        
        ## 로직
        - 채팅방에 진입하면 읽지 않은 메시지를 자동으로 읽음 처리합니다.
        - 최신 메시지가 먼저 오도록 정렬됩니다 (DESC).
        - isMine 필드로 내가 보낸 메시지인지 구분할 수 있습니다.
        - 채팅방 참여자만 메시지를 조회할 수 있습니다.
        - 일반 유저는 자신이 참여한 채팅방만 조회할 수 있습니다.
        - 어드민은 모든 어드민 채팅방을 조회할 수 있습니다.
        - `messageId`가 제공되면 해당 메시지가 포함된 페이지를 자동으로 계산하여 반환합니다.
          검색 결과에서 특정 메시지 위치로 이동할 때 사용합니다.
        
        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        - NOT_FOUND_CHAT_ROOM (404): 채팅방을 찾을 수 없습니다. messageId가 유효하지 않은 경우에도 동일합니다.
        """)
    @GetMapping("/rooms/{chatRoomId}")
    ResponseEntity<ChatMessagePageResponse> getChatRoomMessages(
        @Min(value = 1, message = "페이지 번호는 1 이상이어야 합니다.")
        @RequestParam(name = "page", defaultValue = "1") Integer page,
        @Min(value = 1, message = "페이지 당 항목 수는 1 이상이어야 합니다.")
        @RequestParam(name = "limit", defaultValue = "20", required = false) Integer limit,
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId,
        @RequestParam(name = "messageId", required = false) Integer messageId
    );

    @Operation(summary = "메시지를 전송한다.", description = """
        ## 설명
        - 채팅방에 메시지를 전송합니다.
        
        ## 로직
        - 일반 유저는 자신이 참여한 채팅방에만 메시지를 전송할 수 있습니다.
        - 어드민은 모든 어드민 채팅방에 메시지를 전송할 수 있습니다.
        - 발신자는 자동으로 현재 사용자로 설정됩니다.
        - 수신자는 채팅방 상대방(어드민 채팅방의 경우 일반 유저)으로 자동 설정됩니다.
        - 전송된 메시지 정보를 응답으로 반환합니다.
        
        ## 에러
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @PostMapping("/rooms/{chatRoomId}/messages")
    ResponseEntity<ChatMessageDetailResponse> sendMessage(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatMessageSendRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "채팅 알림 기능 유무를 토글한다.")
    @PostMapping("/rooms/{chatRoomId}/mute")
    ResponseEntity<ChatMuteResponse> toggleChatMute(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );

    @Operation(summary = "내가 보는 채팅방 이름을 수정한다.", description = """
        ## 설명
        - 현재 사용자 기준으로만 보이는 채팅방 이름을 수정합니다.
        - 다른 참여자에게는 영향을 주지 않습니다.
        - null 또는 공백으로 보내면 기본 이름으로 되돌립니다.
        
        ## 에러
        - NOT_FOUND_CHAT_ROOM (404): 채팅방을 찾을 수 없습니다.
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        """)
    @PatchMapping("/rooms/{chatRoomId}/name")
    ResponseEntity<Void> updateChatRoomName(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @Valid @RequestBody ChatRoomNameUpdateRequest request,
        @UserId Integer userId
    );

    @Operation(summary = "채팅방에서 나간다.", description = """
        ## 설명
        - 동아리 채팅방은 나갈 수 없습니다.
        - 1:1 채팅방은 소프트 딜리트 방식으로 나갑니다.
        - 향후 일반 그룹 채팅방은 멤버십 제거 방식으로 나갈 수 있도록 설계합니다.
        
        ## 로직
        - 1:1 채팅방에서 나간 사용자는 기존 메시지를 숨기고 채팅방 목록에서도 제거됩니다.
        - 상대방이 이후 새 메시지를 보내면 나간 사용자는 새 대화처럼 그 메시지부터 다시 보게 됩니다.
        - 사용자가 다시 1:1 채팅을 열면 이전 대화가 아니라 새로 시작한 것처럼 보입니다.
        
        ## 에러
        - CANNOT_LEAVE_GROUP_CHAT_ROOM (400): 동아리 채팅방은 나갈 수 없습니다.
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        - NOT_FOUND_CHAT_ROOM (404): 채팅방을 찾을 수 없습니다.
        """)
    @DeleteMapping("/rooms/{chatRoomId}")
    ResponseEntity<Void> leaveChatRoom(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @UserId Integer userId
    );

    @Operation(summary = "채팅방 멤버를 강퇴한다.", description = """
        ## 설명
        - 그룹 채팅방에서 방장이 특정 멤버를 강퇴합니다.
        
        ## 로직
        - 방장(owner)만 멤버를 강퇴할 수 있습니다.
        - 1:1 채팅방과 동아리 채팅방에서는 강퇴할 수 없습니다.
        - 자기 자신(방장)은 강퇴할 수 없습니다.
        - 이미 채팅방에 없는 멤버는 강퇴할 수 없습니다.
        
        ## 에러
        - NOT_FOUND_CHAT_ROOM (404): 채팅방을 찾을 수 없습니다.
        - FORBIDDEN_CHAT_ROOM_ACCESS (403): 채팅방에 접근할 권한이 없습니다.
        - FORBIDDEN_CHAT_ROOM_KICK (403): 채팅방 방장만 멤버를 강퇴할 수 있습니다.
        - CANNOT_KICK_SELF (400): 자기 자신을 강퇴할 수 없습니다.
        - CANNOT_KICK_ROOM_OWNER (400): 방장은 강퇴할 수 없습니다.
        - CANNOT_KICK_IN_NON_GROUP_ROOM (400): 그룹 채팅방에서만 강퇴할 수 있습니다.
        """)
    @DeleteMapping("/rooms/{chatRoomId}/members/{targetUserId}")
    ResponseEntity<Void> kickMember(
        @PathVariable(value = "chatRoomId") Integer chatRoomId,
        @PathVariable(value = "targetUserId") Integer targetUserId,
        @UserId Integer userId
    );

    @Operation(summary = "그룹 채팅방을 생성한다.", description = """
        ## 설명
        - 여러 유저를 초대하여 그룹 채팅방을 생성합니다.
        
        ## 로직
        - 요청자(방장)를 포함하여 선택된 모든 유저가 참여하는 그룹 채팅방을 생성합니다.
        - 방장은 채팅방을 생성한 사용자입니다.
        
        ## 에러
        - CANNOT_CREATE_CHAT_ROOM_WITH_SELF (400): 자기 자신만으로는 채팅방을 만들 수 없습니다.
        - NOT_FOUND_USER (404): 유저를 찾을 수 없습니다.
        """)
    @PostMapping("/rooms/group")
    ResponseEntity<ChatRoomResponse> createGroupChatRoom(
        @Valid @RequestBody ChatRoomCreateRequest.Group request,
        @UserId Integer userId
    );
}
