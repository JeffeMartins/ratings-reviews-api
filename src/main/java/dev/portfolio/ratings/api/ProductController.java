package dev.portfolio.ratings.api;

import dev.portfolio.ratings.ai.AnalysisService;
import dev.portfolio.ratings.api.Models.*;
import dev.portfolio.ratings.catalog.CatalogService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@Validated
public class ProductController {
  private final CatalogService catalog;
  private final AnalysisService ai;

  public ProductController(CatalogService catalog, AnalysisService ai) {
    this.catalog = catalog;
    this.ai = ai;
  }

  @GetMapping
  @Operation(summary = "List products with bounded pagination")
  public PageView<ProductView> products(
      @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.list(page, size);
  }

  @PostMapping
  @Operation(summary = "Create a product (ADMIN)")
  public ResponseEntity<ProductView> create(@Valid @RequestBody CreateProduct input) {
    var result = catalog.create(input);
    return ResponseEntity.created(URI.create("/api/v1/products/" + result.id())).body(result);
  }

  @GetMapping("/{id}")
  public ProductView product(@PathVariable UUID id) {
    return ProductView.of(catalog.require(id));
  }

  @GetMapping("/by-sku/{sku}")
  @Operation(summary = "Find product by SKU with aggregate stars and paginated reviews/comments")
  public ProductOverview bySku(
      @PathVariable @Pattern(regexp = "[A-Z0-9][A-Z0-9._-]{0,63}") String sku,
      @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.overview(sku, page, size);
  }

  @PostMapping("/by-sku/{sku}/analysis")
  @Operation(summary = "Generate optional AI analysis by SKU (ADMIN)")
  public Analysis analysisBySku(
      @PathVariable @Pattern(regexp = "[A-Z0-9][A-Z0-9._-]{0,63}") String sku) {
    return ai.analyze(catalog.requireSku(sku).getId());
  }

  @GetMapping("/{id}/reviews")
  public PageView<ReviewView> reviews(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") @Min(0) @Max(10000) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return catalog.reviews(id, page, size);
  }

  @PostMapping("/{id}/reviews")
  @Operation(summary = "Create one review per authenticated user and product")
  public ResponseEntity<ReviewView> review(
      @PathVariable UUID id, @Valid @RequestBody CreateReview input, Authentication auth) {
    var result = catalog.review(id, auth.getName(), input);
    return ResponseEntity.created(URI.create("/api/v1/products/" + id + "/reviews")).body(result);
  }

  @DeleteMapping("/{id}/reviews/{reviewId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID id, @PathVariable UUID reviewId, Authentication auth) {
    catalog.delete(
        id,
        reviewId,
        auth.getName(),
        auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
  }

  @GetMapping("/{id}/summary")
  @Operation(summary = "Get aggregate ratings (Redis cache, TTL 30s)")
  public Summary summary(@PathVariable UUID id) {
    return catalog.summary(id);
  }

  @PostMapping("/{id}/analysis")
  @Operation(summary = "Generate optional AI analysis (ADMIN, may incur provider costs)")
  public Analysis analysis(@PathVariable UUID id) {
    return ai.analyze(id);
  }
}
