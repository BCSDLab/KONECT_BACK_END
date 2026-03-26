package gg.agit.konect.unit.global.auth.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;

import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.auth.annotation.Auth;
import gg.agit.konect.global.auth.web.AuthorizationInterceptor;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
class AuthorizationInterceptorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    private AuthorizationInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new AuthorizationInterceptor(userRepository, handlerExceptionResolver);
    }

    @Nested
    @DisplayName("validateRole - 권한 판별 로직")
    class ValidateRole {

        @Test
        @DisplayName("관리자 권한이 있으면 통과한다")
        void adminRolePass() throws Exception {
            // given
            Integer userId = 1;
            User admin = createUser(UserRole.ADMIN);

            given(userRepository.getById(userId)).willReturn(admin);

            // when & then - 예외 없이 통과
            invokeValidateRole(userId, adminOnlyAuth());
        }

        @Test
        @DisplayName("일반 사용자는 관리자 권한이 필요한 API에 접근할 수 없다")
        void userRoleCannotAccessAdminApi() throws Exception {
            // given
            Integer userId = 1;
            User normalUser = createUser(UserRole.USER);

            given(userRepository.getById(userId)).willReturn(normalUser);

            // when & then
            assertThatThrownBy(() -> invokeValidateRole(userId, adminOnlyAuth()))
                .isInstanceOf(InvocationTargetException.class)
                .extracting("cause")
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ApiResponseCode.FORBIDDEN_ROLE_ACCESS);
        }

        @Test
        @DisplayName("다중 권한 중 하나라도 일치하면 통과한다")
        void oneOfMultipleRolesMatch() throws Exception {
            // given
            Integer userId = 1;
            User admin = createUser(UserRole.ADMIN);

            given(userRepository.getById(userId)).willReturn(admin);

            // when & then - 예외 없이 통과
            invokeValidateRole(userId, multiRoleAuth());
        }

        @Test
        @DisplayName("일반 사용자도 USER 권한 API에는 접근할 수 있다")
        void userCanAccessUserApi() throws Exception {
            // given
            Integer userId = 1;
            User normalUser = createUser(UserRole.USER);

            given(userRepository.getById(userId)).willReturn(normalUser);

            // when & then - 예외 없이 통과
            invokeValidateRole(userId, multiRoleAuth());
        }
    }

    private void invokeValidateRole(Integer userId, Auth auth) throws Exception {
        Method method = AuthorizationInterceptor.class.getDeclaredMethod("validateRole", Integer.class, Auth.class);
        method.setAccessible(true);
        method.invoke(interceptor, userId, auth);
    }

    private Auth adminOnlyAuth() throws Exception {
        Method method = AuthMethods.class.getDeclaredMethod("adminOnly");
        return AnnotatedElementUtils.findMergedAnnotation(method, Auth.class);
    }

    private Auth multiRoleAuth() throws Exception {
        Method method = AuthMethods.class.getDeclaredMethod("multiRole");
        return AnnotatedElementUtils.findMergedAnnotation(method, Auth.class);
    }

    private User createUser(UserRole role) {
        return User.builder()
            .role(role)
            .build();
    }

    static class AuthMethods {
        @Auth(roles = {UserRole.ADMIN})
        void adminOnly() {
        }

        @Auth(roles = {UserRole.ADMIN, UserRole.USER})
        void multiRole() {
        }
    }
}
