package org.example.common.interceptor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import static org.junit.jupiter.api.Assertions.*;
public class InternalApiFilterTest {
    @Test
    void identityHeaderDoesNotAuthenticateInternalCaller() throws Exception {
        var request = new MockHttpServletRequest("POST", "/internal/members/7/activate");
        request.setServletPath("/internal/members/7/activate");
        request.addHeader("X-USER-ID", "7");
        var response = new MockHttpServletResponse();
        var forwarded = new AtomicBoolean();
        new InternalApiFilter("fixture-key").doFilter(request, response, (a, b) -> forwarded.set(true));
        assertEquals(401, response.getStatus());
        assertFalse(forwarded.get());
    }
    @Test
    void configuredServiceKeyAuthenticatesCaller() throws Exception {
        var request = new MockHttpServletRequest("POST", "/internal/members/7/activate");
        request.setServletPath("/internal/members/7/activate");
        request.addHeader("X-Internal-Key", "fixture-key");
        var forwarded = new AtomicBoolean();
        new InternalApiFilter("fixture-key").doFilter(request, new MockHttpServletResponse(),
                (a, b) -> forwarded.set(true));
        assertTrue(forwarded.get());
    }
    @Test
    void missingConfigurationFailsClosed() throws Exception {
        var request = new MockHttpServletRequest("POST", "/internal/members/7/activate");
        request.setServletPath("/internal/members/7/activate");
        var response = new MockHttpServletResponse();
        new InternalApiFilter("").doFilter(request, response, (a, b) -> fail());
        assertEquals(503, response.getStatus());
    }
}
