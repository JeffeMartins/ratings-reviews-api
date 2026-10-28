package dev.portfolio.ratings.catalog;

import dev.portfolio.ratings.api.*;
import dev.portfolio.ratings.api.Models.*;
import dev.portfolio.ratings.cache.SummaryCache;
import dev.portfolio.ratings.review.*;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
  private final ProductRepository products;
  private final ReviewRepository reviews;
  private final SummaryCache cache;
  private final ApplicationEventPublisher events;

  public CatalogService(
      ProductRepository products,
      ReviewRepository reviews,
      SummaryCache cache,
      ApplicationEventPublisher events) {
    this.products = products;
    this.reviews = reviews;
    this.cache = cache;
    this.events = events;
  }

  public Product require(UUID id) {
    return products.findById(id).orElseThrow(ApiException::notFound);
  }

  public Product requireSku(String sku) {
    return products.findBySku(sku).orElseThrow(ApiException::notFound);
  }

  public ProductOverview overview(String sku, int page, int size) {
    var product = requireSku(sku);
    return new ProductOverview(
        ProductView.of(product), summary(product.getId()), reviews(product.getId(), page, size));
  }

  @Transactional
  public ProductView create(CreateProduct input) {
    return ProductView.of(
        products.save(new Product(input.sku(), input.name().strip(), input.description().strip())));
  }

  public PageView<ProductView> list(int page, int size) {
    var result =
        products.findAll(PageRequest.of(page, size, Sort.by("createdAt", "id").descending()));
    return new PageView<>(
        result.map(ProductView::of).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  public PageView<ReviewView> reviews(UUID id, int page, int size) {
    require(id);
    var result =
        reviews.findByProductId(
            id, PageRequest.of(page, size, Sort.by("createdAt", "id").descending()));
    return new PageView<>(
        result.map(ReviewView::of).getContent(),
        page,
        size,
        result.getTotalElements(),
        result.getTotalPages());
  }

  @Transactional
  public ReviewView review(UUID id, String author, CreateReview input) {
    require(id);
    var result =
        reviews.saveAndFlush(
            new Review(id, author, input.rating(), input.title().strip(), input.content().strip()));
    events.publishEvent(new ReviewsChanged(id));
    return ReviewView.of(result);
  }

  @Transactional
  public void delete(UUID productId, UUID reviewId, String author, boolean admin) {
    var review =
        reviews
            .findById(reviewId)
            .filter(r -> r.getProductId().equals(productId))
            .orElseThrow(ApiException::notFound);
    if (!admin && !review.getAuthor().equals(author))
      throw new ApiException(
          HttpStatus.FORBIDDEN, "Only the author or an administrator may delete this review");
    reviews.delete(review);
    events.publishEvent(new ReviewsChanged(productId));
  }

  public Summary summary(UUID id) {
    require(id);
    return cache
        .get(id)
        .orElseGet(
            () -> {
              var aggregate = reviews.aggregate(id);
              var result =
                  new Summary(
                      id, aggregate.getTotal(), Math.round(aggregate.getAverage() * 100.0) / 100.0);
              cache.put(result);
              return result;
            });
  }

  public record ReviewsChanged(UUID productId) {}
}
