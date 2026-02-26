package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.web.AuthorizationInterceptor;
import gg.agit.konect.global.auth.web.LoginCheckInterceptor;
import gg.agit.konect.global.auth.web.LoginUserArgumentResolver;
import gg.agit.konect.global.config.CorsProperties;
import gg.agit.konect.global.logging.LoggingProperties;

@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @MockBean
    protected LoginCheckInterceptor loginCheckInterceptor;

    @MockBean
    protected LoginUserArgumentResolver loginUserArgumentResolver;

    @MockBean
    protected AuthorizationInterceptor authorizationInterceptor;

    @MockBean
    protected JwtProvider jwtProvider;

    @MockBean
    protected CorsProperties corsProperties;

    @MockBean
    protected LoggingProperties loggingProperties;

    @BeforeEach
    void setUpCommonMocks() throws Exception {
        given(loginCheckInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(loginUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authorizationInterceptor.preHandle(any(), any(), any())).willReturn(true);
        given(corsProperties.allowedOrigins()).willReturn(List.of("http://localhost:3000"));
        given(loggingProperties.ignoredUrlPatterns()).willReturn(List.of());
    }

    protected void mockLoginUser(Integer userId) throws Exception {
        given(loginUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(userId);
    }
}
