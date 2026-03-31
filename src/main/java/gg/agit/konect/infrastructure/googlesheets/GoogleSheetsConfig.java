package gg.agit.konect.infrastructure.googlesheets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.UserCredentials;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GoogleSheetsConfig {

    private final GoogleSheetsProperties googleSheetsProperties;
    private final ResourceLoader resourceLoader;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (InputStream in = openCredentialsStream()) {
            return GoogleCredentials.fromStream(in)
                .createScoped(Arrays.asList(
                    SheetsScopes.SPREADSHEETS,
                    DriveScopes.DRIVE
                ));
        }
    }

    @Bean
    public Sheets googleSheetsService(
        GoogleCredentials googleCredentials
    ) throws IOException, GeneralSecurityException {
        return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(googleCredentials))
            .setApplicationName(googleSheetsProperties.applicationName())
            .build();
    }

    @Bean
    public Drive googleDriveService(
        GoogleCredentials googleCredentials
    ) throws IOException, GeneralSecurityException {
        return new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(googleCredentials))
            .setApplicationName(googleSheetsProperties.applicationName())
            .build();
    }

    public Drive buildUserDriveService(String refreshToken) throws IOException, GeneralSecurityException {
        UserCredentials credentials = UserCredentials.newBuilder()
            .setClientId(googleSheetsProperties.oauthClientId())
            .setClientSecret(googleSheetsProperties.oauthClientSecret())
            .setRefreshToken(refreshToken)
            .build();

        GoogleCredentials scoped = credentials.createScoped(
            Collections.singletonList(DriveScopes.DRIVE)
        );

        return new Drive.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(scoped))
            .setApplicationName(googleSheetsProperties.applicationName())
            .build();
    }

    private InputStream openCredentialsStream() throws IOException {
        String credentialsPath = googleSheetsProperties.credentialsPath();
        if (credentialsPath != null && credentialsPath.startsWith("classpath:")) {
            Resource resource = resourceLoader.getResource(credentialsPath);
            return resource.getInputStream();
        }
        return new FileInputStream(credentialsPath);
    }
}
