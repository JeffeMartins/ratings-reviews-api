package dev.portfolio.ratings;

import static org.assertj.core.api.Assertions.*;

import dev.portfolio.ratings.review.Review;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReviewTest {
  @Test
  void rejectsRatingsOutsideDomainRange() {
    for (int rating : new int[] {-1, 0, 6, 100})
      assertThatThrownBy(() -> new Review(UUID.randomUUID(), "user", rating, "title", "text"))
          .isInstanceOf(IllegalArgumentException.class);
  }
}
