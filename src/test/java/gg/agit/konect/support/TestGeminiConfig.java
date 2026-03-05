package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import gg.agit.konect.infrastructure.gemini.client.GeminiClient;

@TestConfiguration
public class TestGeminiConfig {

    @Bean
    @Primary
    public GeminiClient geminiClient() {
        GeminiClient mockClient = mock(GeminiClient.class);
        when(mockClient.analyzeIntent(anyString())).thenReturn("UNKNOWN");
        when(mockClient.generateResponse(anyString(), any())).thenReturn("테스트 응답입니다.");
        return mockClient;
    }
}
