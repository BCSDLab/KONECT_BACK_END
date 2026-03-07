package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import gg.agit.konect.infrastructure.claude.client.ClaudeClient;

@TestConfiguration
public class TestClaudeConfig {

    @Bean
    @Primary
    public ClaudeClient claudeClient() {
        ClaudeClient mockClient = mock(ClaudeClient.class);
        when(mockClient.chat(anyString())).thenReturn("테스트 응답입니다.");
        return mockClient;
    }
}
