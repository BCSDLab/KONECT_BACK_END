package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import gg.agit.konect.infrastructure.mcp.client.McpClient;

@TestConfiguration
public class TestMcpConfig {

    @Bean
    @Primary
    public McpClient mcpClient() {
        McpClient mockClient = mock(McpClient.class);
        when(mockClient.executeQuery(anyString())).thenReturn("테스트 쿼리 결과");
        when(mockClient.listTables()).thenReturn(List.of("users", "club", "club_member"));
        when(mockClient.describeTable(anyString())).thenReturn("테스트 테이블 스키마");
        when(mockClient.isHealthy()).thenReturn(true);
        return mockClient;
    }
}
