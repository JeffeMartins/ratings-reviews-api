package dev.portfolio.ratings;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.portfolio.ratings.ai.AnalysisService;
import dev.portfolio.ratings.ai.OpenAiGateway;
import dev.portfolio.ratings.api.Models.*;
import dev.portfolio.ratings.catalog.CatalogService;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class AnalysisServiceTest {
  @Test
  void disabledIntegrationNeverCallsPaidProvider() {
    var catalog = mock(CatalogService.class);
    var gateway = mock(OpenAiGateway.class);
    var service = new AnalysisService(catalog, gateway, new ObjectMapper(), false, "fake-test-key");
    assertThat(service.analyze(UUID.randomUUID()).status()).isEqualTo("DISABLED");
    verifyNoInteractions(gateway);
  }

  @Test
  void emptySampleDoesNotCallProvider() {
    var catalog = mock(CatalogService.class);
    var gateway = mock(OpenAiGateway.class);
    UUID id = UUID.randomUUID();
    when(catalog.reviews(id, 0, 20)).thenReturn(new PageView<>(List.of(), 0, 20, 0, 0));
    var service = new AnalysisService(catalog, gateway, new ObjectMapper(), true, "fake-test-key");
    assertThat(service.analyze(id).status()).isEqualTo("NO_REVIEWS");
    verifyNoInteractions(gateway);
  }

  @Test
  void providerFailureReturnsExplicitUnavailableStatusWithoutLeakingException() {
    var catalog = mock(CatalogService.class);
    var gateway = mock(OpenAiGateway.class);
    UUID id = UUID.randomUUID();
    var review =
        new ReviewView(
            UUID.randomUUID(), id, "private-author", 4, "Title", "Review text", Instant.now());
    when(catalog.reviews(id, 0, 20)).thenReturn(new PageView<>(List.of(review), 0, 20, 1, 1));
    when(gateway.analyze(anyString()))
        .thenAnswer(
            invocation -> {
              assertThat((String) invocation.getArgument(0))
                  .doesNotContain("private-author")
                  .contains("Review text");
              throw new IllegalStateException("sensitive provider error");
            });
    var result =
        new AnalysisService(catalog, gateway, new ObjectMapper(), true, "fake-test-key")
            .analyze(id);
    assertThat(result.status()).isEqualTo("UNAVAILABLE");
    assertThat(result.sampleSize()).isEqualTo(1);
    assertThat(result.text()).doesNotContain("sensitive");
    verify(gateway, times(1)).analyze(anyString());
  }
}
