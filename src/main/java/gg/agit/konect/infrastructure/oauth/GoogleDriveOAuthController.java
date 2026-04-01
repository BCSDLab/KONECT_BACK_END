package gg.agit.konect.infrastructure.oauth;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.global.auth.annotation.PublicApi;
import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.infrastructure.oauth.dto.GoogleDriveAuthorizationUrlResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth/google/drive")
@Tag(name = "(Normal) OAuth - Google Drive")
public class GoogleDriveOAuthController {

    private final GoogleDriveOAuthService googleDriveOAuthService;

    @Operation(summary = "Google Drive 권한 연결 URL 조회")
    @GetMapping("/authorize-url")
    public ResponseEntity<GoogleDriveAuthorizationUrlResponse> getAuthorizationUrl(@UserId Integer userId) {
        String authUrl = googleDriveOAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.ok(new GoogleDriveAuthorizationUrlResponse(authUrl));
    }

    @Operation(summary = "Google Drive 권한 연결 페이지로 리다이렉트")
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@UserId Integer userId) {
        String authUrl = googleDriveOAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    @PublicApi
    @Operation(summary = "Google Drive OAuth callback 처리")
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
        @RequestParam("code") String code,
        @RequestParam("state") String state
    ) {
        googleDriveOAuthService.exchangeAndSaveToken(code, state);
        return ResponseEntity.ok().build();
    }
}
