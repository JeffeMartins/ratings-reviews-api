package dev.portfolio.ratings.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.ratings.api.Models.Analysis;
import dev.portfolio.ratings.catalog.CatalogService;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {
  private final CatalogService catalog;
  private final OpenAiGateway gateway;
  private final ObjectMapper json;
  private final boolean enabled;

  public AnalysisService(
      CatalogService catalog,
      OpenAiGateway gateway,
      ObjectMapper json,
      @Value("${app.ai.enabled}") boolean enabled,
      @Value("${app.ai.api-key}") String key) {
    this.catalog = catalog;
    this.gateway = gateway;
    this.json = json;
    this.enabled = enabled && !key.isBlank();
  }

  public Analysis analyze(UUID id) {
    catalog.require(id);
    if (!enabled)
      return new Analysis(
          id, "DISABLED", "AI integration is disabled or its key is missing.", 0, Instant.now());
    var sample =
        catalog.reviews(id, 0, 20).content().stream()
            .map(r -> Map.of("rating", r.rating(), "title", r.title(), "content", r.content()))
            .toList();
    if (sample.isEmpty())
      return new Analysis(id, "NO_REVIEWS", "No reviews available for analysis.", 0, Instant.now());
    try {
      return new Analysis(
          id,
          "AVAILABLE",
          gateway.analyze(json.writeValueAsString(sample)),
          sample.size(),
          Instant.now());
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Cannot encode review sample", ex);
    } catch (RuntimeException ex) {
      return new Analysis(
          id,
          "UNAVAILABLE",
          "AI provider unavailable or local request limit reached. Try again later.",
          sample.size(),
          Instant.now());
    }
  }
}
