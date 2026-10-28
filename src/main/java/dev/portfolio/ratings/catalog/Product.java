package dev.portfolio.ratings.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class Product {
  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 64)
  private String sku;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(nullable = false, length = 2000)
  private String description;

  @Column(nullable = false)
  private Instant createdAt;

  protected Product() {}

  public Product(String name, String description) {
    this(null, name, description);
  }

  public Product(String sku, String name, String description) {
    this.id = UUID.randomUUID();
    this.sku = sku == null ? "PRD-" + id.toString().toUpperCase(java.util.Locale.ROOT) : sku;
    this.name = name;
    this.description = description;
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public String getSku() {
    return sku;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
