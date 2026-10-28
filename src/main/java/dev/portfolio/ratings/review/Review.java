package dev.portfolio.ratings.review;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
public class Review {
  @Id private UUID id;

  @Column(nullable = false)
  private UUID productId;

  @Column(nullable = false, length = 100)
  private String author;

  @Column(nullable = false)
  private short rating;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 2000)
  private String content;

  @Column(nullable = false)
  private Instant createdAt;

  protected Review() {}

  public Review(UUID productId, String author, int rating, String title, String content) {
    if (rating < 1 || rating > 5)
      throw new IllegalArgumentException("Rating must be between 1 and 5");
    this.id = UUID.randomUUID();
    this.productId = productId;
    this.author = author;
    this.rating = (short) rating;
    this.title = title;
    this.content = content;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getAuthor() {
    return author;
  }

  public int getRating() {
    return rating;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
