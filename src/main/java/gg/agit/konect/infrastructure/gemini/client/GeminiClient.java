package gg.agit.konect.infrastructure.gemini.client;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

import gg.agit.konect.infrastructure.gemini.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiClient {

    private static final String DATABASE_SCHEMA = """
        ## KONECT 서비스 데이터베이스 스키마 (JPQL 엔티티)

        ### User (사용자)
        - id: Integer (PK)
        - email: String (이메일)
        - name: String (이름)
        - studentNumber: String (학번)
        - provider: Provider (GOOGLE, APPLE, NAVER, KAKAO)
        - university: University (소속 대학)
        - createdAt: LocalDateTime (가입일)
        - deletedAt: LocalDateTime (탈퇴일, null이면 활성 사용자)

        ### University (대학교)
        - id: Integer (PK)
        - koreanName: String (한글 이름)
        - englishName: String (영문 이름)

        ### Club (동아리)
        - id: Integer (PK)
        - name: String (동아리 이름)
        - description: String (소개)
        - university: University (소속 대학)
        - createdAt: LocalDateTime (생성일)

        ### ClubMember (동아리 멤버)
        - id.clubId: Integer (동아리 ID)
        - id.userId: Integer (사용자 ID)
        - club: Club
        - user: User
        - clubPosition: ClubPosition (PRESIDENT, VICE_PRESIDENT, MANAGER, MEMBER)
        - createdAt: LocalDateTime (가입일)

        ### ClubRecruitment (동아리 모집 공고)
        - id: Integer (PK)
        - club: Club
        - isAlwaysRecruiting: Boolean (상시 모집 여부)
        - startAt: LocalDateTime (모집 시작일)
        - endAt: LocalDateTime (모집 종료일)
        - content: String (공고 내용)

        ### ClubApply (동아리 지원)
        - id: Integer (PK)
        - club: Club
        - user: User
        - status: ClubApplyStatus (PENDING, APPROVED, REJECTED)
        - createdAt: LocalDateTime (지원일)

        ### Schedule (일정)
        - id: Integer (PK)
        - title: String (제목)
        - startedAt: LocalDateTime (시작일)
        - endedAt: LocalDateTime (종료일)
        - scheduleType: ScheduleType (UNIVERSITY, CLUB, COUNCIL)

        ### StudyTimeDaily (일별 공부 시간)
        - id: Integer (PK)
        - user: User
        - studySeconds: Long (공부 시간, 초)
        - date: LocalDate (날짜)

        ### StudyTimeMonthly (월별 공부 시간)
        - id: Integer (PK)
        - user: User
        - studySeconds: Long (공부 시간, 초)
        - yearMonth: String (년월, YYYY-MM)

        ## 중요 규칙
        - 활성 사용자 조회 시: WHERE u.deletedAt IS NULL
        - 현재 모집 중: WHERE cr.isAlwaysRecruiting = true
          OR (cr.startAt <= CURRENT_TIMESTAMP AND cr.endAt >= CURRENT_TIMESTAMP)
        """;

    private static final String QUERY_GENERATION_PROMPT = """
        당신은 KONECT 서비스의 데이터 분석 AI입니다.
        사용자의 자연어 질문을 분석하여 JPQL 쿼리를 생성해주세요.

        %s

        ## 규칙
        1. 반드시 유효한 JPQL SELECT 쿼리만 생성하세요.
        2. DELETE, UPDATE, INSERT 등 데이터 변경 쿼리는 절대 생성하지 마세요.
        3. 쿼리만 반환하고, 다른 설명은 포함하지 마세요.
        4. 쿼리가 불가능한 질문이면 "UNSUPPORTED"만 반환하세요.

        ## 예시
        질문: "가입된 사용자 수 알려줘"
        쿼리: SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL

        질문: "현재 모집 중인 동아리 몇개야?"
        쿼리: SELECT COUNT(cr) FROM ClubRecruitment cr WHERE cr.isAlwaysRecruiting = true
          OR (cr.startAt <= CURRENT_TIMESTAMP AND cr.endAt >= CURRENT_TIMESTAMP)

        질문: "동아리별 멤버 수 알려줘"
        쿼리: SELECT c.name, COUNT(cm) FROM ClubMember cm JOIN cm.club c GROUP BY c.name

        질문: "오늘 날씨 어때?"
        쿼리: UNSUPPORTED

        ---
        사용자 질문: %s

        쿼리:
        """;

    private static final String RESPONSE_GENERATION_PROMPT = """
        당신은 KONECT 서비스의 친절한 AI 어시스턴트입니다.
        아래 정보를 바탕으로 사용자에게 자연스럽고 친절한 한국어로 응답해주세요.
        이모지를 적절히 사용하여 친근하게 답변해주세요.

        사용자 질문: %s
        조회된 데이터: %s

        응답:
        """;

    private final GeminiProperties geminiProperties;

    public GeminiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    public String generateQuery(String userQuery) {
        String prompt = String.format(QUERY_GENERATION_PROMPT, DATABASE_SCHEMA, userQuery);
        String result = callGemini(prompt).trim();

        // 코드 블록 제거
        if (result.startsWith("```")) {
            result = result.replaceAll("```\\w*\\n?", "").trim();
        }

        return result;
    }

    public String generateResponse(String userQuery, String data) {
        String prompt = String.format(RESPONSE_GENERATION_PROMPT, userQuery, data);
        return callGemini(prompt);
    }

    private String callGemini(String prompt) {
        try (VertexAI vertexAI = new VertexAI(
            geminiProperties.projectId(),
            geminiProperties.location()
        )) {
            GenerativeModel model = new GenerativeModel(geminiProperties.model(), vertexAI);
            GenerateContentResponse response = model.generateContent(prompt);
            return ResponseHandler.getText(response);
        } catch (IOException e) {
            log.error("Gemini API 호출 실패", e);
            return "UNSUPPORTED";
        }
    }
}
