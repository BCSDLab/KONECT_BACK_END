package gg.agit.konect.infrastructure.googlesheets;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
    public Sheets googleSheetsService() throws IOException, GeneralSecurityException {
        InputStream in = new FileInputStream(googleSheetsProperties.credentialsPath());
        GoogleCredentials credentials = GoogleCredentials.fromStream(in)
            .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        return new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            new HttpCredentialsAdapter(credentials))
            .setApplicationName(googleSheetsProperties.applicationName())
            .build();
    }
}
