package org.example.common.interceptor;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class InternalApiFilter extends OncePerRequestFilter {
    private final String key;
    public InternalApiFilter(@Value("${internal.api-key:}") String key) {
        this.key = key;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !org.springframework.web.util.UrlPathHelper.defaultInstance.getPathWithinApplication(request)
                .startsWith("/internal/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String supplied = request.getHeader("X-Internal-Key");
        if (key.isBlank()) {
            response.sendError(503);
            return;
        }
        if (supplied == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(401);
            return;
        }
        chain.doFilter(request, response);
    }
}
