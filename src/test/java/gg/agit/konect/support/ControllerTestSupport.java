package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.global.auth.annotation.UserId;
import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.web.AuthorizationInterceptor;
import gg.agit.konect.global.auth.web.LoginCheckInterceptor;
import gg.agit.konect.global.auth.web.LoginUserArgumentResolver;
import gg.agit.konect.global.config.CorsProperties;
import gg.agit.konect.global.logging.LoggingProperties;

// @WebMvcTest는 웹 레이어만 로드하므로 DB 접근 없이 컨트롤러 단위 테스트가 가능하다.
// 하위 클래스에서 @WebMvcTest(TargetController.class)를 선언하여 테스트 대상을 지정한다.
@ActiveProfiles("test")
@Import({TestSecurityConfig.class})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockitoBean
    protected LoginCheckInterceptor loginCheckInterceptor;

    @MockitoBean
    protected LoginUserArgumentResolver loginUserArgumentResolver;

    @MockitoBean
    protected AuthorizationInterceptor authorizationInterceptor;

    @MockitoBean
    protected JwtProvider jwtProvider;

    @MockitoBean
    protected CorsProperties corsProperties;

    @MockitoBean
    protected LoggingProperties loggingProperties;

    @BeforeEach
    void setUpCommonMocks() throws Exception {
        given(loginCheckInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(loginUserArgumentResolver.supportsParameter(any(MethodParameter.class))).willAnswer(invocation -> {
            MethodParameter parameter = invocation.getArgument(0);
            boolean hasAnnotation = parameter.hasParameterAnnotation(UserId.class);
            boolean isIntegerType = Integer.class.equals(parameter.getParameterType())
                || int.class.equals(parameter.getParameterType());
            return hasAnnotation && isIntegerType;
        });
        given(authorizationInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(corsProperties.allowedOrigins()).willReturn(List.of("http://localhost:3000"));
        given(loggingProperties.ignoredUrlPatterns()).willReturn(List.of());
    }

    protected void mockLoginUser(Integer userId) throws Exception {
        given(loginUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userId);
    }

    protected ResultActions performGet(String url) throws Exception {
        return mockMvc.perform(get(url)
            .contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performGet(String url, MultiValueMap<String, String> params) throws Exception {
        return mockMvc.perform(get(url)
            .params(params)
            .contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPost(String url) throws Exception {
        return mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performPut(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performPatch(String url, Object body) throws Exception {
        return mockMvc.perform(patch(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions performDelete(String url) throws Exception {
        return mockMvc.perform(delete(url)
            .contentType(MediaType.APPLICATION_JSON));
    }

    protected ResultActions performDelete(String url, Object body) throws Exception {
        return mockMvc.perform(delete(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(body)));
    }

    protected MockHttpServletRequestBuilder withAuth(MockHttpServletRequestBuilder builder) {
        return builder.header("Authorization", "Bearer test-token");
    }
}
