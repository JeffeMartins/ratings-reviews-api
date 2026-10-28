package dev.portfolio.ratings.api;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
public class CsrfController {
  @GetMapping("/api/v1/csrf")
  @Operation(summary = "Initialize XSRF-TOKEN cookie before write operations")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("cookieName", "XSRF-TOKEN", "headerName", token.getHeaderName());
  }
}
