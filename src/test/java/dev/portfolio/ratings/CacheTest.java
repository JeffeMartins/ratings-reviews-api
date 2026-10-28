package dev.portfolio.ratings;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.ratings.cache.SummaryCache;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

class CacheTest {
  @Test
  void redisFailureBecomesCacheMissAndOpensCircuit() {
    var redis = mock(StringRedisTemplate.class);
    when(redis.opsForValue()).thenThrow(new IllegalStateException("Redis unavailable"));
    var registry = CircuitBreakerRegistry.ofDefaults();
    var breaker =
        registry.circuitBreaker(
            "redis",
            io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                .minimumNumberOfCalls(2)
                .slidingWindowSize(2)
                .build());
    var cache = new SummaryCache(redis, new ObjectMapper(), registry, new SimpleMeterRegistry());
    for (int i = 0; i < 3; i++) assertThat(cache.get(UUID.randomUUID())).isEmpty();
    assertThat(breaker.getState())
        .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);
    verify(redis, times(2)).opsForValue();
  }
}
