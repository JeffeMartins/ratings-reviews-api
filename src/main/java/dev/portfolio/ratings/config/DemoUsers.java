package dev.portfolio.ratings.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.UserDetailsManager;

/** Additional identities only for the isolated, explicitly enabled portfolio demo. */
@Configuration
@Profile("demo")
public class DemoUsers {
  @Bean
  ApplicationRunner demoReviewers(
      org.springframework.security.core.userdetails.UserDetailsService service,
      @Value("${app.security.reviewer-password}") String password) {
    return args -> {
      var users = (UserDetailsManager) service;
      var encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
      for (var name :
          new String[] {"demo-ana", "demo-bruno", "demo-carla", "demo-diego", "demo-elisa"}) {
        users.createUser(
            User.withUsername(name).password(encoder.encode(password)).roles("REVIEWER").build());
      }
    };
  }
}
