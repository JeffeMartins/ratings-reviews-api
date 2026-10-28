package dev.portfolio.ratings.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;

/** Supports a plain cookie token in JavaScript headers and masked tokens in request attributes. */
final class BrowserCsrfHandler implements CsrfTokenRequestHandler {
  private final CsrfTokenRequestHandler masked = new XorCsrfTokenRequestAttributeHandler();
  private final CsrfTokenRequestHandler cookieHeader = new CsrfTokenRequestAttributeHandler();

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> token) {
    masked.handle(request, response, token);
    token.get(); // Materialize the deferred cookie for Swagger and other browser clients.
  }

  @Override
  public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken token) {
    var resolver =
        StringUtils.hasText(request.getHeader(token.getHeaderName())) ? cookieHeader : masked;
    return resolver.resolveCsrfTokenValue(request, token);
  }
}
