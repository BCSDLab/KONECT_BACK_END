package gg.agit.konect.unit.infrastructure.claude.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.infrastructure.claude.client.DatabaseSchemaCache;
import gg.agit.konect.support.ServiceTestSupport;

class DatabaseSchemaCacheTest extends ServiceTestSupport {

    private static final String FALLBACK_SCHEMA =
        "DB 스키마 요약 조회에 실패했습니다. list_tables와 describe_table 도구로 다시 확인하세요.";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DatabaseSchemaCache databaseSchemaCache;

    @Test
    @DisplayName("정상 조회 시 DB 스키마 요약을 생성하고 서버 생명주기 동안 캐시한다")
    void getSchemaSummaryFormatsAndCachesSchema() {
        // given
        givenTableRows(table("users", "서비스 사용자 계정"));
        givenColumnRows(
            column("users", "id", "int", "NO", "PRI", ""),
            column("users", "email", "varchar(100)", "NO", "", "사용자 이메일")
        );

        // when
        String firstSummary = databaseSchemaCache.getSchemaSummary();
        String secondSummary = databaseSchemaCache.getSchemaSummary();

        // then
        assertThat(firstSummary).isEqualTo(secondSummary);
        assertThat(firstSummary)
            .contains("- users: 서비스 사용자 계정")
            .contains("    - id int NOT NULL PRI")
            .contains("    - email varchar(100) NOT NULL: 사용자 이메일");
        verify(jdbcTemplate, times(1)).query(tableSchemaSql(), rowMapper());
        verify(jdbcTemplate, times(1)).query(columnSchemaSql(), rowMapper());
    }

    @Test
    @DisplayName("스키마 조회 실패 시 짧은 재시도 대기 시간 동안 fallback을 반환한다")
    void getSchemaSummaryReturnsFallbackWithoutRepeatedDbHitsWhenSchemaLoadFails() {
        // given
        given(jdbcTemplate.query(tableSchemaSql(), rowMapper()))
            .willThrow(new QueryTimeoutException("timeout"));

        // when
        String firstSummary = databaseSchemaCache.getSchemaSummary();
        String secondSummary = databaseSchemaCache.getSchemaSummary();

        // then
        assertThat(firstSummary).isEqualTo(FALLBACK_SCHEMA);
        assertThat(secondSummary).isEqualTo(FALLBACK_SCHEMA);
        verify(jdbcTemplate, times(1)).query(tableSchemaSql(), rowMapper());
    }

    @Test
    @DisplayName("테이블 목록이 비어 있으면 성공 캐시로 저장하지 않는다")
    void getSchemaSummaryDoesNotCacheEmptySchemaAsSuccess() {
        // given
        AtomicInteger tableQueryCount = new AtomicInteger();
        given(jdbcTemplate.query(tableSchemaSql(), rowMapper()))
            .willAnswer(invocation -> {
                if (tableQueryCount.getAndIncrement() == 0) {
                    return List.of();
                }
                return mapRows(invocation.getArgument(1), table("users", "서비스 사용자 계정"));
            });
        givenColumnRows(column("users", "id", "int", "NO", "PRI", ""));

        // when
        String firstSummary = databaseSchemaCache.getSchemaSummary();
        ReflectionTestUtils.setField(databaseSchemaCache, "retrySchemaLoadAt", Instant.EPOCH);
        String secondSummary = databaseSchemaCache.getSchemaSummary();

        // then
        assertThat(firstSummary).isEqualTo(FALLBACK_SCHEMA);
        assertThat(secondSummary).contains("- users: 서비스 사용자 계정");
        verify(jdbcTemplate, times(2)).query(tableSchemaSql(), rowMapper());
        verify(jdbcTemplate, times(1)).query(columnSchemaSql(), rowMapper());
    }

    private void givenTableRows(ResultSet... rows) {
        given(jdbcTemplate.query(tableSchemaSql(), rowMapper()))
            .willAnswer(invocation -> mapRows(invocation.getArgument(1), rows));
    }

    private void givenColumnRows(ResultSet... rows) {
        given(jdbcTemplate.query(columnSchemaSql(), rowMapper()))
            .willAnswer(invocation -> mapRows(invocation.getArgument(1), rows));
    }

    private List<Object> mapRows(RowMapper<Object> rowMapper, ResultSet... rows) throws SQLException {
        List<Object> mappedRows = new java.util.ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            mappedRows.add(rowMapper.mapRow(rows[i], i));
        }
        return mappedRows;
    }

    private ResultSet table(String tableName, String tableComment) {
        ResultSet resultSet = mock(ResultSet.class);
        try {
            given(resultSet.getString("table_name")).willReturn(tableName);
            given(resultSet.getString("table_comment")).willReturn(tableComment);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return resultSet;
    }

    private ResultSet column(
        String tableName,
        String columnName,
        String columnType,
        String isNullable,
        String columnKey,
        String columnComment
    ) {
        ResultSet resultSet = mock(ResultSet.class);
        try {
            given(resultSet.getString("table_name")).willReturn(tableName);
            given(resultSet.getString("column_name")).willReturn(columnName);
            given(resultSet.getString("column_type")).willReturn(columnType);
            given(resultSet.getString("is_nullable")).willReturn(isNullable);
            given(resultSet.getString("column_key")).willReturn(columnKey);
            given(resultSet.getString("column_comment")).willReturn(columnComment);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return resultSet;
    }

    private boolean isTableSchemaSql(String sql) {
        return sql != null && sql.contains("information_schema.tables");
    }

    private boolean isColumnSchemaSql(String sql) {
        return sql != null && sql.contains("information_schema.columns");
    }

    private String tableSchemaSql() {
        return argThat(this::isTableSchemaSql);
    }

    private String columnSchemaSql() {
        return argThat(this::isColumnSchemaSql);
    }

    @SuppressWarnings("unchecked")
    private RowMapper<Object> rowMapper() {
        return org.mockito.ArgumentMatchers.any(RowMapper.class);
    }
}
