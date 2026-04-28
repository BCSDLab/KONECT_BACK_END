package gg.agit.konect.domain.chat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatInvitableUsersResponse;
import gg.agit.konect.domain.chat.enums.ChatInviteSortBy;
import gg.agit.konect.domain.chat.repository.ChatInviteQueryRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatInviteService {

    private static final String ETC_SECTION_NAME = "기타";

    private final ChatInviteQueryRepository chatInviteQueryRepository;
    private final UserRepository userRepository;

    public ChatInvitableUsersResponse getInvitableUsers(
        Integer userId,
        String query,
        ChatInviteSortBy sortBy,
        Integer page,
        Integer limit
    ) {
        userRepository.getById(userId);
        PageRequest pageRequest = PageRequest.of(page - 1, limit);

        if (sortBy == ChatInviteSortBy.CLUB) {
            return getInvitableUsersGroupedByClub(userId, query, pageRequest);
        }

        Page<User> filteredUserEntitiesPage = chatInviteQueryRepository.findInvitableUsers(userId, query, pageRequest);

        // 응답 DTO는 채팅 초대 화면에서 바로 쓰는 최소 필드만 유지한다.
        List<ChatInvitableUsersResponse.InvitableUser> filteredUsers = filteredUserEntitiesPage.getContent().stream()
            .map(ChatInvitableUsersResponse.InvitableUser::from)
            .toList();

        // 응답 메타(total/current page 정보)는 유지하면서 내용만 DTO로 치환한다.
        Page<ChatInvitableUsersResponse.InvitableUser> filteredUsersPage = new PageImpl<>(
            filteredUsers,
            pageRequest,
            filteredUserEntitiesPage.getTotalElements()
        );

        return ChatInvitableUsersResponse.forNameSort(filteredUsersPage);
    }

    private ChatInvitableUsersResponse getInvitableUsersGroupedByClub(
        Integer userId,
        String query,
        PageRequest pageRequest
    ) {
        // CLUB 정렬은 DB가 현재 페이지에 들어갈 userId까지 잘라 오고,
        // 서비스는 그 결과를 섹션 응답으로만 복원한다.
        Page<Integer> pagedUserIds = chatInviteQueryRepository.findInvitableUserIdsGroupedByClub(
            userId,
            query,
            pageRequest
        );

        if (pagedUserIds.isEmpty()) {
            return ChatInvitableUsersResponse.forClubSort(
                new PageImpl<>(List.of(), pageRequest, pagedUserIds.getTotalElements()),
                List.of()
            );
        }

        // IN 조회는 정렬 순서를 보장하지 않으므로, DB가 정한 userId 페이지 순서대로 다시 조립한다.
        Map<Integer, User> pagedUserMap = userRepository.findAllByIdIn(pagedUserIds.getContent()).stream()
            .collect(Collectors.toMap(User::getId, user -> user));

        List<ChatInvitableUsersResponse.InvitableUser> pagedUsers = pagedUserIds.getContent().stream()
            .map(pagedUserMap::get)
            .filter(Objects::nonNull)
            .map(ChatInvitableUsersResponse.InvitableUser::from)
            .toList();

        Page<ChatInvitableUsersResponse.InvitableUser> pagedInvitableUsers = new PageImpl<>(
            pagedUsers,
            pageRequest,
            pagedUserIds.getTotalElements()
        );

        record SectionKey(Integer clubId, String clubName) {
        }

        Map<Integer, Integer> representativeClubByUserId = new HashMap<>();
        Map<Integer, String> representativeClubNames = new HashMap<>();
        // 현재 페이지 사용자에 대해서만 대표 동아리를 다시 구해도,
        // userId 자체는 이미 대표 동아리 기준으로 정렬돼 있으므로 페이지 경계는 유지된다.
        chatInviteQueryRepository.findSharedClubMemberships(userId, pagedUserIds.getContent()).stream()
            .forEach(clubMember -> {
                representativeClubNames.putIfAbsent(clubMember.getClub().getId(), clubMember.getClub().getName());
                representativeClubByUserId.putIfAbsent(clubMember.getUser().getId(), clubMember.getClub().getId());
            });

        // 대표 동아리가 없는 사용자는 기타 섹션으로 떨어지고,
        // 같은 대표 동아리를 가진 사용자끼리만 현재 페이지 sections[]로 묶는다.
        Map<SectionKey, List<ChatInvitableUsersResponse.InvitableUser>> sectionMap = new LinkedHashMap<>();
        pagedUsers.forEach(user -> {
            Integer representativeClubId = representativeClubByUserId.get(user.userId());
            String clubName = representativeClubId == null
                ? ETC_SECTION_NAME
                : representativeClubNames.get(representativeClubId);
            SectionKey key = new SectionKey(representativeClubId, clubName);
            sectionMap.computeIfAbsent(key, ignored -> new ArrayList<>())
                .add(user);
        });

        List<ChatInvitableUsersResponse.InvitableSection> sections = sectionMap.entrySet().stream()
            .map(entry -> new ChatInvitableUsersResponse.InvitableSection(
                entry.getKey().clubId(),
                entry.getKey().clubName(),
                entry.getValue()
            ))
            .toList();

        return ChatInvitableUsersResponse.forClubSort(pagedInvitableUsers, sections);
    }
}
