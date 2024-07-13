package red.sigil.playlists;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN;

@SpringBootApplication
@EnableWebSecurity
@EnableTransactionManagement
@EnableJpaRepositories
@EnableScheduling
public class Boot {

  public static void main(String[] args) {
    SpringApplication.run(Boot.class, args);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    var cspPolicy = "default-src 'self' ; form-action 'self' https://accounts.google.com ; frame-ancestors 'none' ;";

    return http
        .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
        .oauth2Login(withDefaults())
        .headers(h -> h
            .referrerPolicy(referrer -> referrer.policy(SAME_ORIGIN))
            .contentSecurityPolicy(csp -> csp.policyDirectives(cspPolicy))
        )
        .build();
  }
}
