package dev.portfolio.ratings.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

class DemoUsersTest {
  @Test
  void demoConfigurationIsAbsentFromDefaultProfile() {
    try (var context = new AnnotationConfigApplicationContext()) {
      context.register(DemoUsers.class);
      context.refresh();
      assertThat(context.getBeansOfType(DemoUsers.class)).isEmpty();
    }
  }

  @Test
  void demoAuthorsAreDistinctReviewersWithHashedPasswords() throws Exception {
    var users = new InMemoryUserDetailsManager();
    String password = "test-only-demo-password-strong";
    new DemoUsers().demoReviewers(users, password).run(null);
    for (String name :
        new String[] {"demo-ana", "demo-bruno", "demo-carla", "demo-diego", "demo-elisa"}) {
      var user = users.loadUserByUsername(name);
      assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_REVIEWER");
      assertThat(user.getPassword()).isNotEqualTo(password);
      assertThat(
              PasswordEncoderFactories.createDelegatingPasswordEncoder()
                  .matches(password, user.getPassword()))
          .isTrue();
    }
  }
}
