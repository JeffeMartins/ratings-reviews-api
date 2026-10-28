package dev.portfolio.ratings.cache;

import dev.portfolio.ratings.catalog.CatalogService.ReviewsChanged;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CacheInvalidation {
  private final SummaryCache cache;

  public CacheInvalidation(SummaryCache cache) {
    this.cache = cache;
  }

  @TransactionalEventListener
  public void afterCommit(ReviewsChanged event) {
    cache.evict(event.productId());
  }
}
