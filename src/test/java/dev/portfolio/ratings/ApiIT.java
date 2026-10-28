package dev.portfolio.ratings;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;

@SpringBootTest(
    properties = {
      "app.security.admin-password=test-admin-password-strong",
      "app.security.reviewer-password=test-reviewer-password-strong"
    })
@AutoConfigureMockMvc
@Testcontainers
class ApiIT {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);
    r.add("spring.data.redis.host", redis::getHost);
    r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired StringRedisTemplate cache;

  String createProduct() throws Exception {
    var response =
        mvc.perform(
                post("/api/v1/products")
                    .with(httpBasic("admin", "test-admin-password-strong"))
                    .contentType("application/json")
                    .content("{\"name\":\"Headphone\",\"description\":\"Wireless headphones\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(response.getResponse().getContentAsString()).path("id").asText();
  }

  String reviewBody(int rating) {
    return "{\"rating\":"
        + rating
        + ",\"title\":\"Good sound\",\"content\":\"Comfortable fit and clear sound.\"}";
  }

  @Test
  void fullLifecycleUsesCacheAndInvalidatesAfterCommit() throws Exception {
    String id = createProduct();
    String path = "/api/v1/products/" + id;
    mvc.perform(get(path + "/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalReviews").value(0));
    String key = "ratings:summary:v1:" + id;
    assertThat(cache.opsForValue().get(key)).isNotNull();
    var created =
        mvc.perform(
                post(path + "/reviews")
                    .with(user("alice").roles("REVIEWER"))
                    .contentType("application/json")
                    .content(reviewBody(5)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.author").value("alice"))
            .andReturn();
    String reviewId = json.readTree(created.getResponse().getContentAsString()).path("id").asText();
    assertThat(cache.opsForValue().get(key)).isNull();
    mvc.perform(get(path + "/summary"))
        .andExpect(jsonPath("$.totalReviews").value(1))
        .andExpect(jsonPath("$.averageRating").value(5.0));
    mvc.perform(
            post(path + "/reviews")
                .with(user("alice").roles("REVIEWER"))
                .contentType("application/json")
                .content(reviewBody(4)))
        .andExpect(status().isConflict());
    mvc.perform(delete(path + "/reviews/" + reviewId).with(user("bob").roles("REVIEWER")))
        .andExpect(status().isForbidden());
    mvc.perform(delete(path + "/reviews/" + reviewId).with(user("alice").roles("REVIEWER")))
        .andExpect(status().isNoContent());
    mvc.perform(get(path + "/summary")).andExpect(jsonPath("$.totalReviews").value(0));
  }

  @Test
  void authorizationValidationAndDocs() throws Exception {
    mvc.perform(post("/api/v1/products").contentType("application/json").content("{}"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/v1/products")
                .with(user("reviewer").roles("REVIEWER"))
                .contentType("application/json")
                .content("{}"))
        .andExpect(status().isForbidden());
    mvc.perform(
            post("/api/v1/products")
                .with(user("admin").roles("ADMIN"))
                .contentType("application/json")
                .content("{\"name\":\" \",\"description\":\"x\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/products?size=1000")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/products/not-a-uuid")).andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/products/" + UUID.randomUUID())).andExpect(status().isNotFound());
    mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists());
    String id = createProduct();
    mvc.perform(
            post("/api/v1/products/" + id + "/reviews")
                .with(user("alice").roles("REVIEWER"))
                .contentType("application/json")
                .content(reviewBody(6)))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/products/" + id + "/analysis").with(user("admin").roles("ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DISABLED"));
  }
}
