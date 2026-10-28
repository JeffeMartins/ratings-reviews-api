package dev.portfolio.ratings.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** A bounded, per-instance budget; use a gateway for distributed per-client quotas. */
@Component
public class RequestLimitFilter extends OncePerRequestFilter {
  private final RateLimiter limiter =
      RateLimiter.of(
          "api",
          RateLimiterConfig.custom()
              .limitForPeriod(300)
              .limitRefreshPeriod(Duration.ofSeconds(1))
              .timeoutDuration(Duration.ZERO)
              .build());

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (!limiter.acquirePermission()) {
      response.setStatus(429);
      response.setHeader("Retry-After", "1");
      response.setContentType("application/problem+json");
      response
          .getWriter()
          .write(
              "{\"title\":\"Too Many Requests\",\"status\":429,\"detail\":\"API instance request budget exceeded\"}");
      return;
    }
    chain.doFilter(request, response);
  }
}
