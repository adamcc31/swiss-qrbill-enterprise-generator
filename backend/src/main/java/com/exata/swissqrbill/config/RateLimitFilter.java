package com.exata.swissqrbill.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Map<String, Bucket> generateDownloadBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> validateBuckets = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        
        // Only apply to our QR-bill API endpoints
        if (path.startsWith("/api/v1/qrbill")) {
            String ip = httpRequest.getRemoteAddr();
            
            if (path.endsWith("/generate") || path.endsWith("/download")) {
                Bucket bucket = generateDownloadBuckets.computeIfAbsent(ip, this::createNewGenerateDownloadBucket);
                if (!tryConsume(bucket)) {
                    sendRateLimitError(httpResponse, 60);
                    return;
                }
            } else if (path.endsWith("/validate")) {
                Bucket bucket = validateBuckets.computeIfAbsent(ip, this::createNewValidateBucket);
                if (!tryConsume(bucket)) {
                    sendRateLimitError(httpResponse, 60);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(Bucket bucket) {
        try {
            return bucket.tryConsume(1);
        } catch (Exception e) {
            // Quality constraint: Rate limiter must never throw uncaught exceptions
            // degrade gracefully (allow request through, log warning)
            log.warn("Rate limiter failed internally, allowing request through: {}", e.getMessage());
            return true;
        }
    }

    private Bucket createNewGenerateDownloadBucket(String ip) {
        // 20 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket createNewValidateBucket(String ip) {
        // 60 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.classic(60, Refill.intervally(60, Duration.ofMinutes(1))))
                .build();
    }

    private void sendRateLimitError(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.getWriter().write("{"
                + "\"success\":false,"
                + "\"errorCode\":\"RATE_LIMIT_EXCEEDED\","
                + "\"message\":\"Too many requests. Please wait before retrying.\","
                + "\"retryAfterSeconds\":" + retryAfterSeconds
                + "}");
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
