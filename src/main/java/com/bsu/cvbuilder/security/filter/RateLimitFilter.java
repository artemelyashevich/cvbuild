package com.bsu.cvbuilder.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Per-IP limit shared by all application instances. Idle limiters expire in Redis, so memory stays bounded.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String KEY_PREFIX = "cvbuilder:rate-limit:ip:";
    private static final long REQUESTS_PER_INTERVAL = 10;
    private static final Duration INTERVAL = Duration.ofSeconds(1);
    private static final Duration IDLE_TTL = Duration.ofMinutes(5);

    private final RedissonClient redissonClient;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (isAllowed(request.getRemoteAddr())) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.getWriter().write("Too many requests");
        }
    }

    private boolean isAllowed(String ip) {
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(KEY_PREFIX + ip);
            limiter.trySetRate(RateType.OVERALL, REQUESTS_PER_INTERVAL, INTERVAL, IDLE_TTL);
            return limiter.tryAcquire();
        } catch (Exception e) {
            // Fail open: an unavailable Redis must not take the whole API down.
            log.error("Rate limiter unavailable, letting request from {} through", ip, e);
            return true;
        }
    }
}
