package gg.agit.konect.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import gg.agit.konect.global.auth.jwt.JwtProvider;
import gg.agit.konect.global.auth.web.AuthorizationInterceptor;
import gg.agit.konect.global.auth.web.LoginCheckInterceptor;
import gg.agit.konect.global.auth.web.LoginUserArgumentResolver;
import gg.agit.konect.global.config.CorsProperties;
import gg.agit.konect.global.logging.LoggingProperties;
import jakarta.persistence.EntityManager;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@Import({TestSecurityConfig.class, TestJpaConfig.class})
@EnableAutoConfiguration(exclude = {
    OAuth2ClientAutoConfiguration.class
})
@TestPropertySource(locations = "classpath:.env.test.properties", properties = {
    "spring.config.import=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.url=jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.properties.javax.persistence.validation.mode=none",
    "spring.flyway.enabled=false",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.security.enabled=false",
    "logging.ignored-url-patterns="
})
public abstract class ControllerTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected EntityManager entityManager;

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

    protected <T> T persist(T entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    protected void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
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
