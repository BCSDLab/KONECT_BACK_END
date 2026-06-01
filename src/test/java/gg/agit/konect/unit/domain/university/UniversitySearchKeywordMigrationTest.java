package gg.agit.konect.unit.domain.university;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class UniversitySearchKeywordMigrationTest {

    private static final List<String> SEEDED_UNIVERSITY_NAMES = List.of(
        "가톨릭대학교",
        "건국대학교",
        "경북대학교",
        "경희대학교",
        "고려대학교",
        "광주과학기술원",
        "단국대학교",
        "대구경북과학기술원",
        "동국대학교",
        "부산대학교",
        "서강대학교",
        "서울과학기술대학교",
        "서울대학교",
        "서울시립대학교",
        "성균관대학교",
        "연세대학교",
        "울산과학기술원",
        "육군사관학교",
        "이화여자대학교",
        "전남대학교",
        "중앙대학교",
        "충남대학교",
        "충북대학교",
        "포항공과대학교",
        "한국공학대학교",
        "한국과학기술원",
        "한국교통대학교",
        "한국기술교육대학교",
        "한국외국어대학교",
        "한국체육대학교",
        "한국항공대학교",
        "한국해양대학교",
        "해군사관학교",
        "홍익대학교"
    );

    private static final int EXPECTED_KEYWORD_COUNT = 58;

    @Test
    void seedUniversitySearchKeywords() throws Exception {
        JdbcTemplate jdbcTemplate = createJdbcTemplate("seedSuccess");
        createUniversityTable(jdbcTemplate);
        insertUniversities(jdbcTemplate, SEEDED_UNIVERSITY_NAMES);

        executeMigration(jdbcTemplate, "db/migration/V86__create_university_search_keyword.sql");
        executeMigration(jdbcTemplate, "db/migration/V87__seed_university_search_keywords.sql");

        Integer keywordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM university_search_keyword",
            Integer.class
        );
        Integer koreatechAliasCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM university_search_keyword keyword
                JOIN university ON university.id = keyword.university_id
                WHERE university.korean_name = '한국기술교육대학교'
                    AND keyword.keyword IN ('한기대', '코리아텍', 'koreatech')
                """,
            Integer.class
        );
        Integer seoulTechAliasCount = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM university_search_keyword keyword
                JOIN university ON university.id = keyword.university_id
                WHERE university.korean_name = '서울과학기술대학교'
                    AND keyword.keyword IN ('과기대', '서울과기대')
                """,
            Integer.class
        );

        assertThat(keywordCount).isEqualTo(EXPECTED_KEYWORD_COUNT);
        assertThat(koreatechAliasCount).isEqualTo(3);
        assertThat(seoulTechAliasCount).isEqualTo(2);
    }

    @Test
    void seedOnlyExistingUniversitySearchKeywords() throws Exception {
        JdbcTemplate jdbcTemplate = createJdbcTemplate("seedOnlyExistingUniversity");
        createUniversityTable(jdbcTemplate);
        insertUniversities(jdbcTemplate, List.of("한국기술교육대학교"));
        executeMigration(jdbcTemplate, "db/migration/V86__create_university_search_keyword.sql");

        executeMigration(jdbcTemplate, "db/migration/V87__seed_university_search_keywords.sql");

        Integer keywordCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM university_search_keyword",
            Integer.class
        );
        List<String> keywords = jdbcTemplate.queryForList(
            """
                SELECT keyword.keyword
                FROM university_search_keyword keyword
                JOIN university ON university.id = keyword.university_id
                WHERE university.korean_name = '한국기술교육대학교'
                """,
            String.class
        );

        assertThat(keywordCount).isEqualTo(3);
        assertThat(keywords).containsExactlyInAnyOrder("한기대", "코리아텍", "koreatech");
    }

    private JdbcTemplate createJdbcTemplate(String databaseName) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return new JdbcTemplate(dataSource);
    }

    private void createUniversityTable(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.execute("""
            CREATE TABLE university
            (
                id          INT AUTO_INCREMENT PRIMARY KEY,
                korean_name VARCHAR(255) NOT NULL
            )
            """);
    }

    private void insertUniversities(JdbcTemplate jdbcTemplate, List<String> universityNames) {
        for (String universityName : universityNames) {
            jdbcTemplate.update("INSERT INTO university (korean_name) VALUES (?)", universityName);
        }
    }

    private void executeMigration(JdbcTemplate jdbcTemplate, String path) throws SQLException {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new ClassPathResource(path), "UTF-8"));
        }
    }
}
