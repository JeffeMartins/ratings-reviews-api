package dev.portfolio.ratings.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
public class SecurityConfig {
  @Bean
  UserDetailsService users(
      @Value("${app.security.admin-password}") String admin,
      @Value("${app.security.reviewer-password}") String reviewer) {
    if (admin.length() < 16 || reviewer.length() < 16)
      throw new IllegalArgumentException("Configure passwords with at least 16 characters");
    var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    return new InMemoryUserDetailsManager(
        User.withUsername("admin")
            .password(encoder.encode(admin))
            .roles("ADMIN", "REVIEWER")
            .build(),
        User.withUsername("reviewer").password(encoder.encode(reviewer)).roles("REVIEWER").build());
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    return http.csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .csrfTokenRequestHandler(new BrowserCsrfHandler()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .requestCache(c -> c.disable())
        .headers(h -> h.frameOptions(f -> f.deny()).contentTypeOptions(Customizer.withDefaults()))
        .authorizeHttpRequests(
            a ->
                a.dispatcherTypeMatchers(DispatcherType.ERROR)
                    .permitAll()
                    .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/csrf")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/**")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST, "/api/v1/products", "/api/v1/products/*/analysis")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.POST, "/api/v1/products/*/reviews")
                    .hasRole("REVIEWER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/products/*/reviews/*")
                    .hasRole("REVIEWER")
                    .anyRequest()
                    .denyAll())
        .httpBasic(Customizer.withDefaults())
        .build();
  }

  @Bean
  OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Ratings & Reviews API")
                .version("v1")
                .description(
                    "Public reads. Use Authorize with admin or reviewer for protected operations."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "basicAuth",
                    new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
        .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
  }
}
