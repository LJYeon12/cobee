package org.example.common.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class SyncKeyInterceptorTest {

    private SyncKeyInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new SyncKeyInterceptor();
        ReflectionTestUtils.setField(interceptor, "secretKey", "unit-test-sync-key");
    }

    @Test
    void acceptsMatchingSyncKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Sync-Key", "unit-test-sync-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "wrong-key")
    void rejectsMissingEmptyOrWrongSyncKey(String key) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (key != null) {
            request.addHeader("X-Sync-Key", key);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(interceptor.preHandle(request, response, new Object())).isFalse();
        assertThat(response.getStatus()).isEqualTo(401);
    }
}
