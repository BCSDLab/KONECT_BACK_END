package gg.agit.konect.infrastructure.slack.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicQueryExecutor {

    private static final int MAX_RESULTS = 100;
    private static final int DISPLAY_LIMIT = 10;

    private final EntityManager entityManager;

    public String executeQuery(String jpqlQuery) {
        try {
            validateQuery(jpqlQuery);

            Query query = entityManager.createQuery(jpqlQuery);
            query.setMaxResults(MAX_RESULTS);

            List<?> results = query.getResultList();

            return formatResults(results);
        } catch (Exception e) {
            log.error("쿼리 실행 실패: query={}", jpqlQuery, e);
            return "쿼리 실행 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private void validateQuery(String query) {
        String upperQuery = query.toUpperCase().trim();

        // SELECT 쿼리만 허용
        if (!upperQuery.startsWith("SELECT")) {
            throw new IllegalArgumentException("SELECT 쿼리만 허용됩니다.");
        }

        // 위험한 키워드 차단
        String[] dangerousKeywords = {
            "DELETE", "UPDATE", "INSERT", "DROP", "TRUNCATE",
            "ALTER", "CREATE", "GRANT", "REVOKE", "EXEC", "EXECUTE"
        };

        for (String keyword : dangerousKeywords) {
            if (upperQuery.contains(keyword)) {
                throw new IllegalArgumentException("허용되지 않는 키워드: " + keyword);
            }
        }
    }

    private String formatResults(List<?> results) {
        if (results.isEmpty()) {
            return "조회 결과가 없습니다.";
        }

        Object first = results.get(0);

        // 단일 값 (COUNT 등)
        if (first instanceof Number || first instanceof String) {
            if (results.size() == 1) {
                return "결과: " + first.toString();
            }
            return "결과 목록: " + results.toString();
        }

        // Object[] (여러 컬럼 조회)
        if (first instanceof Object[]) {
            StringBuilder sb = new StringBuilder();
            sb.append("총 ").append(results.size()).append("건 조회됨\n");
            int count = 0;
            for (Object row : results) {
                if (count >= DISPLAY_LIMIT) {
                    sb.append("... 외 ").append(results.size() - DISPLAY_LIMIT).append("건");
                    break;
                }
                Object[] cols = (Object[])row;
                sb.append("- ");
                for (int i = 0; i < cols.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(cols[i]);
                }
                sb.append("\n");
                count++;
            }
            return sb.toString();
        }

        // 엔티티 객체
        StringBuilder sb = new StringBuilder();
        sb.append("총 ").append(results.size()).append("건 조회됨");
        return sb.toString();
    }
}
