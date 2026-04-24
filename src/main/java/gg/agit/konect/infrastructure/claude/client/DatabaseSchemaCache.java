package gg.agit.konect.infrastructure.claude.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatabaseSchemaCache {

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

    public DatabaseSchemaCache(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getSchemaSummary() {
        String currentSchema = cachedSchema;
        if (currentSchema != null) {
            return currentSchema;
        }

        synchronized (this) {
            if (cachedSchema != null) {
                return cachedSchema;
            }

            try {
                cachedSchema = loadSchemaSummary();
                return cachedSchema;
            } catch (DataAccessException e) {
                log.error("Failed to load database schema summary", e);
                return "DB 스키마 요약 조회에 실패했습니다. list_tables와 describe_table 도구로 다시 확인하세요.";
            }
        }
    }

    private String loadSchemaSummary() {
        List<TableSchema> tables = jdbcTemplate.query(
            TABLE_SCHEMA_SQL,
            (rs, rowNum) -> new TableSchema(
                rs.getString("table_name"),
                normalizeComment(rs.getString("table_comment"))
            )
        );
        List<ColumnSchema> columns = jdbcTemplate.query(
            COLUMN_SCHEMA_SQL,
            (rs, rowNum) -> new ColumnSchema(
                rs.getString("table_name"),
                rs.getString("column_name"),
                rs.getString("column_type"),
                normalizeComment(rs.getString("is_nullable")),
                normalizeComment(rs.getString("column_key")),
                normalizeComment(rs.getString("column_comment"))
            )
        );

        if (tables.isEmpty()) {
            log.warn("Database schema cache loaded an empty schema summary");
            return "현재 DB 스키마를 조회했지만 테이블 정보를 찾지 못했습니다.";
        }

        Map<String, StringBuilder> columnsByTable = new LinkedHashMap<>();
        for (ColumnSchema column : columns) {
            columnsByTable.computeIfAbsent(column.tableName(), ignored -> new StringBuilder())
                .append("    - ")
                .append(column.columnName())
                .append(" ")
                .append(column.columnType());

            if ("NO".equals(column.isNullable())) {
                columnsByTable.get(column.tableName()).append(" NOT NULL");
            }
            if (!column.columnKey().isBlank()) {
                columnsByTable.get(column.tableName()).append(" ").append(column.columnKey());
            }
            if (!column.comment().isBlank()) {
                columnsByTable.get(column.tableName()).append(": ").append(column.comment());
            }
            columnsByTable.get(column.tableName()).append('\n');
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

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return "";
        }
        return comment.trim();
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
