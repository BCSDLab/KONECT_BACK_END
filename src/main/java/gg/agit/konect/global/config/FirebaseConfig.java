package gg.agit.konect.global.config;

import java.io.FileInputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {

    private final String credentialsPath;

    public FirebaseConfig(@Value("${app.firebase.credentials-path}") String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new IllegalStateException("Firebase credentials path is required.");
        }
        this.credentialsPath = credentialsPath;
    }

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            return FirebaseApp.initializeApp(options);
        }
    }
}
