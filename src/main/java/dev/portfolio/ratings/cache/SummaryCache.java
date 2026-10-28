package dev.portfolio.ratings.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.ratings.api.Models.Summary;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.util.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class SummaryCache {
  private final StringRedisTemplate redis;
  private final ObjectMapper json;
  private final io.github.resilience4j.circuitbreaker.CircuitBreaker breaker;
  private final MeterRegistry metrics;

  public SummaryCache(
      StringRedisTemplate redis,
      ObjectMapper json,
      CircuitBreakerRegistry registry,
      MeterRegistry metrics) {
    this.redis = redis;
    this.json = json;
    this.breaker = registry.circuitBreaker("redis");
    this.metrics = metrics;
  }

  public Optional<Summary> get(UUID id) {
    try {
      String value = breaker.executeSupplier(() -> redis.opsForValue().get(key(id)));
      metrics.counter("ratings.cache", "result", value == null ? "miss" : "hit").increment();
      return value == null ? Optional.empty() : Optional.of(json.readValue(value, Summary.class));
    } catch (Exception ex) {
      metrics.counter("ratings.cache", "result", "error").increment();
      return Optional.empty();
    }
  }

  public void put(Summary value) {
    try {
      String encoded = json.writeValueAsString(value);
      breaker.executeRunnable(
          () -> redis.opsForValue().set(key(value.productId()), encoded, Duration.ofSeconds(30)));
    } catch (Exception ex) {
      metrics.counter("ratings.cache", "result", "error").increment();
    }
  }

  public void evict(UUID id) {
    try {
      breaker.executeRunnable(() -> redis.delete(key(id)));
    } catch (Exception ex) {
      metrics.counter("ratings.cache", "result", "error").increment();
    }
  }

  private String key(UUID id) {
    return "ratings:summary:v1:" + id;
  }
}
