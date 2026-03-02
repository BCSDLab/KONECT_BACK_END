package gg.agit.konect.infrastructure.gemini.client;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;

import gg.agit.konect.infrastructure.gemini.config.GeminiProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiClient {

    private static final String INTENT_ANALYSIS_PROMPT = """
        당신은 KONECT 서비스의 데이터 분석 AI입니다.
        사용자의 질문을 분석하여 다음 중 하나의 쿼리 타입만 반환하세요.
        반드시 아래 목록 중 하나의 값만 반환하고, 다른 텍스트는 포함하지 마세요.

        가능한 쿼리 타입:
        - USER_COUNT: 가입된 사용자 수, 회원 수, 유저 수 관련 질문
        - CLUB_COUNT: 전체 동아리 수, 동아리 개수 관련 질문
        - CLUB_RECRUITING_COUNT: 현재 모집 중인 동아리 수, 모집 현황 관련 질문
        - CLUB_MEMBER_TOTAL_COUNT: 전체 동아리원 수, 동아리 멤버 총 인원 관련 질문
        - UNKNOWN: 위 항목에 해당하지 않는 질문

        예시:
        - "가입된 사용자 수 알려줘" -> USER_COUNT
        - "현재 모집 중인 동아리 몇개야?" -> CLUB_RECRUITING_COUNT
        - "전체 동아리 수는?" -> CLUB_COUNT
        - "동아리원이 총 몇명이야?" -> CLUB_MEMBER_TOTAL_COUNT
        - "오늘 날씨 어때?" -> UNKNOWN
        - "특정 사용자 이메일 알려줘" -> UNKNOWN

        사용자 질문: %s

        쿼리 타입:
        """;

    private static final String RESPONSE_GENERATION_PROMPT = """
        당신은 KONECT 서비스의 친절한 AI 어시스턴트입니다.
        아래 정보를 바탕으로 사용자에게 자연스럽고 친절한 한국어로 응답해주세요.
        이모지를 적절히 사용하여 친근하게 답변해주세요.
        응답은 간결하게 2-3문장으로 작성해주세요.

        사용자 질문: %s
        조회된 데이터: %s

        응답:
        """;

    private final GeminiProperties geminiProperties;
    private VertexAI vertexAI;
    private GenerativeModel generativeModel;

    public GeminiClient(GeminiProperties geminiProperties) {
        this.geminiProperties = geminiProperties;
    }

    @PostConstruct
    public void init() {
        this.vertexAI = new VertexAI(
            geminiProperties.projectId(),
            geminiProperties.location()
        );
        this.generativeModel = new GenerativeModel(geminiProperties.model(), vertexAI);
        log.info("GeminiClient 초기화 완료: project={}, location={}, model={}",
            geminiProperties.projectId(),
            geminiProperties.location(),
            geminiProperties.model()
        );
    }

    @PreDestroy
    public void destroy() {
        if (vertexAI != null) {
            try {
                vertexAI.close();
                log.info("GeminiClient 리소스 해제 완료");
            } catch (Exception e) {
                log.warn("GeminiClient 리소스 해제 중 오류", e);
            }
        }
    }

    public String analyzeIntent(String userQuery) {
        String prompt = String.format(INTENT_ANALYSIS_PROMPT, userQuery);
        String result = callGemini(prompt);
        if (result == null) {
            return "UNKNOWN";
        }
        return result.trim().toUpperCase();
    }

    public String generateResponse(String userQuery, String data) {
        String prompt = String.format(RESPONSE_GENERATION_PROMPT, userQuery, data);
        String result = callGemini(prompt);
        return result != null ? result : "응답을 생성할 수 없습니다.";
    }

    private String callGemini(String prompt) {
        if (generativeModel == null) {
            log.error("GenerativeModel이 초기화되지 않았습니다.");
            return null;
        }

        try {
            GenerateContentResponse response = generativeModel.generateContent(prompt);
            return ResponseHandler.getText(response);
        } catch (IOException e) {
            log.error("Gemini API 호출 실패", e);
            return null;
        }
    }
}
