package com.retaildashboard.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * API Rate Limiting 설정.
 * Redis 기반 슬라이딩 윈도우 방식으로 IP별 요청 수를 제한합니다.
 * Redis 미사용 시 인메모리 폴백을 제공합니다.
 *
 * - 일반 API: 분당 60회
 * - 인증 API: 분당 10회 (브루트포스 방지)
 * - 내보내기 API: 분당 5회 (리소스 보호)
 *
 * Requirements: 15.1, 15.2, 2.6, 2.7
 */
@Configuration
@Slf4j
public class RateLimitConfig {

    private static final int DEFAULT_LIMIT = 60;
    private static final int AUTH_LIMIT = 10;
    private static final int EXPORT_LIMIT = 5;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilter(RedisTemplate<String, Object> redisTemplate) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RateLimitFilter(redisTemplate));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Rate Limiting 필터.
     * Redis를 사용하여 IP별 요청 수를 추적하고, 제한 초과 시 429 응답을 반환합니다.
     */
    @Slf4j
    static class RateLimitFilter extends OncePerRequestFilter {

        private final RedisTemplate<String, Object> redisTemplate;
        private final Map<String, TokenBucket> inMemoryBuckets = new ConcurrentHashMap<>();

        RateLimitFilter(RedisTemplate<String, Object> redisTemplate) {
            this.redisTemplate = redisTemplate;
        }

        @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain) throws ServletException, IOException {

            String clientIp = getClientIp(request);
            String path = request.getRequestURI();
            int limit = resolveLimit(path);

            boolean allowed;
            try {
                allowed = checkRateLimitRedis(clientIp, path, limit);
            } catch (Exception e) {
                // Redis 장애 시 인메모리 폴백
                log.debug("Redis rate limit check failed, using in-memory fallback: {}", e.getMessage());
                allowed = checkRateLimitInMemory(clientIp, path, limit);
            }

            if (!allowed) {
                log.warn("Rate limit exceeded: ip={}, path={}, limit={}/min", clientIp, path, limit);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"retryAfterSeconds\":60}");
                return;
            }

            // Rate limit 헤더 추가
            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            filterChain.doFilter(request, response);
        }

        /**
         * Redis 기반 rate limit 체크 (슬라이딩 윈도우).
         */
        private boolean checkRateLimitRedis(String clientIp, String path, int limit) {
            String key = "rate_limit:" + resolveCategory(path) + ":" + clientIp;

            Long currentCount = redisTemplate.opsForValue().increment(key);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, WINDOW);
            }

            return currentCount != null && currentCount <= limit;
        }

        /**
         * 인메모리 폴백 rate limit 체크.
         */
        private boolean checkRateLimitInMemory(String clientIp, String path, int limit) {
            String key = resolveCategory(path) + ":" + clientIp;
            TokenBucket bucket = inMemoryBuckets.computeIfAbsent(key,
                    k -> new TokenBucket(limit, WINDOW.toMillis()));
            return bucket.tryConsume();
        }

        /**
         * 경로별 rate limit 값을 결정합니다.
         */
        private int resolveLimit(String path) {
            if (path.startsWith("/api/v1/auth/")) {
                return AUTH_LIMIT;
            }
            if (path.startsWith("/api/v1/export/")) {
                return EXPORT_LIMIT;
            }
            return DEFAULT_LIMIT;
        }

        /**
         * 경로를 카테고리로 분류합니다.
         */
        private String resolveCategory(String path) {
            if (path.startsWith("/api/v1/auth/")) return "auth";
            if (path.startsWith("/api/v1/export/")) return "export";
            return "general";
        }

        /**
         * 클라이언트 IP를 추출합니다 (프록시 헤더 지원).
         */
        private String getClientIp(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            return request.getRemoteAddr();
        }
    }

    /**
     * 인메모리 토큰 버킷 (Redis 폴백용).
     */
    static class TokenBucket {
        private final int maxTokens;
        private final long windowMillis;
        private final AtomicInteger tokens;
        private volatile long windowStart;

        TokenBucket(int maxTokens, long windowMillis) {
            this.maxTokens = maxTokens;
            this.windowMillis = windowMillis;
            this.tokens = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }

        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                tokens.set(0);
                windowStart = now;
            }
            return tokens.incrementAndGet() <= maxTokens;
        }
    }
}
