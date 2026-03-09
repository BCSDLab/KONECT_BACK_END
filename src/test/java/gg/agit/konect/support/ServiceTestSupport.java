package gg.agit.konect.support;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

// Spring Context 없이 Mockito만으로 서비스 로직을 단위 테스트한다.
// 하위 클래스에서 @Mock, @InjectMocks를 사용하여 의존성을 주입한다.
@ExtendWith(MockitoExtension.class)
public abstract class ServiceTestSupport {
}
