package com.bsu.cvbuilder.security;

import com.bsu.cvbuilder.security.filter.RateLimitFilter;
import com.bsu.cvbuilder.support.RedisTestSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private static RedissonClient instanceA;
    private static RedissonClient instanceB;

    @BeforeAll
    static void setUp() {
        instanceA = RedisTestSupport.newRedisson();
        instanceB = RedisTestSupport.newRedisson();
    }

    @AfterAll
    static void tearDown() {
        instanceA.shutdown();
        instanceB.shutdown();
    }

    @Test
    @DisplayName("the per-IP limit is shared by all instances: the 11th request within a second is rejected")
    void limitIsSharedAcrossInstances() throws Exception {
        String ip = "10.0.0." + UUID.randomUUID().hashCode();
        RateLimitFilter filterA = new RateLimitFilter(instanceA);
        RateLimitFilter filterB = new RateLimitFilter(instanceB);

        for (int i = 0; i < 10; i++) {
            assertEquals(200, call(i % 2 == 0 ? filterA : filterB, ip), "request " + (i + 1));
        }

        assertEquals(429, call(filterA, ip));
        assertEquals(429, call(filterB, ip));
        assertEquals(200, call(filterA, ip + "-other"));
    }

    @Test
    @DisplayName("an unavailable Redis lets requests through instead of failing the API")
    void redisDown_FailsOpen() throws Exception {
        RedissonClient broken = mock(RedissonClient.class);
        when(broken.getRateLimiter(anyString())).thenThrow(new IllegalStateException("redis down"));

        assertEquals(200, call(new RateLimitFilter(broken), "10.0.0.1"));
    }

    private static int call(RateLimitFilter filter, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response.getStatus();
    }
}
