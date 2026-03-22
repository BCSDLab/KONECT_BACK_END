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
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/oauth/google/drive")
public class GoogleDriveOAuthController {

    private final GoogleDriveOAuthService googleDriveOAuthService;

    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize(@UserId Integer userId) {
        String authUrl = googleDriveOAuthService.buildAuthorizationUrl(userId);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(authUrl)).build();
    }

    @PublicApi
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
        @RequestParam("code") String code,
        @RequestParam("state") String state
    ) {
        googleDriveOAuthService.exchangeAndSaveToken(code, state);
        return ResponseEntity.ok().build();
    }
}
