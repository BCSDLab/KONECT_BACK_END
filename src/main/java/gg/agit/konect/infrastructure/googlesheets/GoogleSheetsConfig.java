package gg.agit.konect.infrastructure.googlesheets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class GoogleSheetsConfig {

    private final GoogleSheetsProperties googleSheetsProperties;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (InputStream in = new FileInputStream(googleSheetsProperties.credentialsPath())) {
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
}
