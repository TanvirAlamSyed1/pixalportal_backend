package com.pixalportal.backend.security;

import io.github.bucket4j.Bucket;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitInterceptor implements HandlerInterceptor {

    // A simple map to store buckets per IP address
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = request.getRemoteAddr();
        
        Bucket bucket = cache.computeIfAbsent(clientIp, k -> Bucket.builder()
                .addLimit(limit -> limit.capacity(5).refillGreedy(5, java.time.Duration.ofMinutes(1)))
                .build());

        if (bucket.tryConsume(1)) {
            return true; // Allowed
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please slow down.");
            return false; // Blocked
        }
    }
}