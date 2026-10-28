package dev.portfolio.ratings.review;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
  Page<Review> findByProductId(UUID productId, Pageable pageable);

  @Query(
      "select count(r) as total, coalesce(avg(r.rating), 0.0) as average from Review r where r.productId = :productId")
  RatingAggregate aggregate(UUID productId);

  interface RatingAggregate {
    long getTotal();

    double getAverage();
  }
}
