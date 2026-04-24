package gg.agit.konect.infrastructure.claude.client;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatabaseSchemaCache {

    private static final Duration FAILURE_RETRY_INTERVAL = Duration.ofSeconds(30);
    private static final String FALLBACK_SCHEMA =
        "DB 스키마 요약 조회에 실패했습니다. list_tables와 describe_table 도구로 다시 확인하세요.";
    private static final String TABLE_SCHEMA_SQL = """
        SELECT table_name, table_comment
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_type = 'BASE TABLE'
          AND table_name <> 'flyway_schema_history'
        ORDER BY table_name
        """;
    private static final String COLUMN_SCHEMA_SQL = """
        SELECT table_name,
               column_name,
               column_type,
               is_nullable,
               column_key,
               column_comment
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name <> 'flyway_schema_history'
        ORDER BY table_name, ordinal_position
        """;

    private final JdbcTemplate jdbcTemplate;
    private volatile String cachedSchema;
    private volatile Instant retrySchemaLoadAt = Instant.EPOCH;

    public DatabaseSchemaCache(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getSchemaSummary() {
        String currentSchema = cachedSchema;
        if (currentSchema != null) {
            return currentSchema;
        }
        Instant now = Instant.now();
        if (now.isBefore(retrySchemaLoadAt)) {
            return FALLBACK_SCHEMA;
        }

        synchronized (this) {
            if (cachedSchema != null) {
                return cachedSchema;
            }
            now = Instant.now();
            if (now.isBefore(retrySchemaLoadAt)) {
                return FALLBACK_SCHEMA;
            }

            try {
                cachedSchema = loadSchemaSummary();
                return cachedSchema;
            } catch (DataAccessException e) {
                log.error("Failed to load database schema summary", e);
                retrySchemaLoadAt = now.plus(FAILURE_RETRY_INTERVAL);
                return FALLBACK_SCHEMA;
            }
        }
    }

    private String loadSchemaSummary() {
        List<TableSchema> tables = jdbcTemplate.query(
            TABLE_SCHEMA_SQL,
            (rs, rowNum) -> new TableSchema(
                rs.getString("table_name"),
                nullSafeTrim(rs.getString("table_comment"))
            )
        );

        if (tables.isEmpty()) {
            log.warn("Database schema cache loaded an empty schema summary");
            throw new DataRetrievalFailureException("Database schema summary is empty");
        }

        List<ColumnSchema> columns = jdbcTemplate.query(
            COLUMN_SCHEMA_SQL,
            (rs, rowNum) -> new ColumnSchema(
                rs.getString("table_name"),
                rs.getString("column_name"),
                rs.getString("column_type"),
                nullSafeTrim(rs.getString("is_nullable")),
                nullSafeTrim(rs.getString("column_key")),
                nullSafeTrim(rs.getString("column_comment"))
            )
        );

        Map<String, StringBuilder> columnsByTable = new LinkedHashMap<>();
        for (ColumnSchema column : columns) {
            StringBuilder tableColumns = columnsByTable.computeIfAbsent(
                column.tableName(),
                ignored -> new StringBuilder()
            );
            tableColumns
                .append("    - ")
                .append(column.columnName())
                .append(" ")
                .append(column.columnType());

            if ("NO".equals(column.isNullable())) {
                tableColumns.append(" NOT NULL");
            }
            if (!column.columnKey().isBlank()) {
                tableColumns.append(" ").append(column.columnKey());
            }
            if (!column.comment().isBlank()) {
                tableColumns.append(": ").append(column.comment());
            }
            tableColumns.append('\n');
        }

        StringBuilder summary = new StringBuilder();
        summary.append("현재 DB 스키마 요약입니다. 테이블 comment를 우선 신뢰하고, 존재하는 테이블/컬럼만 사용하세요.\n");
        for (TableSchema table : tables) {
            summary.append("- ")
                .append(table.tableName())
                .append(": ")
                .append(table.comment().isBlank() ? "설명 없음" : table.comment())
                .append('\n');
            StringBuilder tableColumns = columnsByTable.get(table.tableName());
            if (tableColumns != null) {
                summary.append(tableColumns);
            }
        }

        return summary.toString();
    }

    private String nullSafeTrim(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }

    private record TableSchema(
        String tableName,
        String comment
    ) {
    }

    private record ColumnSchema(
        String tableName,
        String columnName,
        String columnType,
        String isNullable,
        String columnKey,
        String comment
    ) {
    }
}
