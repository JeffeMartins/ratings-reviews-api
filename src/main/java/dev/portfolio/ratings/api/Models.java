package dev.portfolio.ratings.api;

import dev.portfolio.ratings.catalog.Product;
import dev.portfolio.ratings.review.Review;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

public final class Models {
  private Models() {}

  public record CreateProduct(
      @Pattern(regexp = "[A-Z0-9][A-Z0-9._-]{0,63}") String sku,
      @NotBlank @Size(max = 160) String name,
      @NotBlank @Size(max = 2000) String description) {}

  public record CreateReview(
      @Min(1) @Max(5) int rating,
      @NotBlank @Size(max = 160) String title,
      @NotBlank @Size(max = 2000) String content) {}

  public record ProductView(
      UUID id, String sku, String name, String description, Instant createdAt) {
    public static ProductView of(Product p) {
      return new ProductView(
          p.getId(), p.getSku(), p.getName(), p.getDescription(), p.getCreatedAt());
    }
  }

  public record ReviewView(
      UUID id,
      UUID productId,
      String author,
      int rating,
      String title,
      String content,
      Instant createdAt) {
    public static ReviewView of(Review r) {
      return new ReviewView(
          r.getId(),
          r.getProductId(),
          r.getAuthor(),
          r.getRating(),
          r.getTitle(),
          r.getContent(),
          r.getCreatedAt());
    }
  }

  public record PageView<T>(
      List<T> content, int page, int size, long totalElements, int totalPages) {}

  public record Summary(UUID productId, long totalReviews, double averageRating) {}

  public record ProductOverview(
      ProductView product, Summary summary, PageView<ReviewView> reviews) {}

  public record Analysis(
      UUID productId, String status, String text, int sampleSize, Instant generatedAt) {}
}
