package gg.agit.konect.domain.club.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubMemberSheetSyncResponse;
import gg.agit.konect.domain.club.dto.ClubSheetIdUpdateRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "(Normal) Club - Sheet: \uc778\uba85\ubd80 \uc2dc\ud2b8 \ub3d9\uae30\ud654")
@RequestMapping("/clubs")
public interface ClubMemberSheetApi {

    @Operation(
        summary = "\ub3d9\uc544\ub9ac \uad6c\uae00 \uc2a4\ud504\ub808\ub4dc\uc2dc\ud2b8 ID\ub97c \ub4f1\ub85d/\uc218\uc815\ud55c\ub2e4.",
        description = """
            \ub3d9\uc544\ub9ac \uc6b4\uc601\uc9c4 \uc774\uc0c1\ub9cc \ub4f1\ub85d\ud560 \uc218 \uc788\uc2b5\ub2c8\ub2e4.
            \uc774\ud6c4 \uc778\uba85\ubd80 \ub3d9\uae30\ud654 API \ud638\ucd9c \uc2dc \uc800\uc7a5\ub41c ID\ub97c \uc0ac\uc6a9\ud569\ub2c8\ub2e4.

            ## \uc0ac\uc804 \uc870\uac74
            - \uc11c\ube44\uc2a4 \uacc4\uc815 \uc774\uba54\uc77c\uc744 \ud574\ub2f9 \uc2a4\ud504\ub808\ub4dc\uc2dc\ud2b8\uc5d0 \ud3b8\uc9d1\uc790\ub85c \uacf5\uc720\ud574\uc57c \ud569\ub2c8\ub2e4.

            ## \uc5d0\ub7ec
            - FORBIDDEN_CLUB_MANAGER_ACCESS (403): \ub3d9\uc544\ub9ac \ub9e4\ub2c8\uc800 \uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.
            - NOT_FOUND_CLUB (404): \ub3d9\uc544\ub9ac\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.
            """
    )
    @PutMapping("/{clubId}/sheet")
    ResponseEntity<Void> updateSheetId(
        @PathVariable(name = "clubId") Integer clubId,
        @Valid @RequestBody ClubSheetIdUpdateRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "\ub3d9\uc544\ub9ac \uc778\uba85\ubd80\ub97c \uad6c\uae00 \uc2a4\ud504\ub808\ub4dc\uc2dc\ud2b8\ub85c \ub0b4\ubcf4\ub0b8\ub2e4.",
        description = """
            \ub3d9\uc544\ub9ac \uc6b4\uc601\uc9c4 \uc774\uc0c1\ub9cc \ub0b4\ubcf4\ub0bc \uc218 \uc788\uc2b5\ub2c8\ub2e4.
            \uae30\uc874 \uc2dc\ud2b8 \ub370\uc774\ud130\ub97c \ucd08\uae30\ud654\ud558\uace0 \ud604\uc7ac DB \uae30\uc900 \uc804\uccb4 \ud68c\uc6d0 \ubaa9\ub85d\uc744 \ub36e\uc5b4\uc37c\ub2c8\ub2e4.

            ## \uc2dc\ud2b8 \ucf5c\ub7fc \uc21c\uc11c
            \uc774\ub984 | \ud559\ubc88 | \uc774\uba54\uc77c | \uc804\ud654\ubc88\ud638 | \uc9c1\uccb8 | \uac00\uc785\uc77c

            ## \uc5d0\ub7ec
            - FORBIDDEN_CLUB_MANAGER_ACCESS (403): \ub3d9\uc544\ub9ac \ub9e4\ub2c8\uc800 \uad8c\ud55c\uc774 \uc5c6\uc2b5\ub2c8\ub2e4.
            - NOT_FOUND_CLUB (404): \ub3d9\uc544\ub9ac\ub97c \ucc3e\uc744 \uc218 \uc5c6\uc2b5\ub2c8\ub2e4.
            - NOT_FOUND_CLUB_SHEET_ID (404): \ub4f1\ub85d\ub41c \uc2a4\ud504\ub808\ub4dc\uc2dc\ud2b8 ID\uac00 \uc5c6\uc2b5\ub2c8\ub2e4.
            - FAILED_SYNC_GOOGLE_SHEET (500): \uad6c\uae00 \uc2a4\ud504\ub808\ub4dc\uc2dc\ud2b8 \ub3d9\uae30\ud654\uc5d0 \uc2e4\ud328\ud588\uc2b5\ub2c8\ub2e4.
            """
    )
    @PostMapping("/{clubId}/members/sheet-sync")
    ResponseEntity<ClubMemberSheetSyncResponse> syncMembersToSheet(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );
}
