package red.sigil.playlists;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableWebSecurity
@EnableJpaRepositories
@EnableTransactionManagement
@EnableScheduling
public class Boot {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Boot.class, args);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Configuration
  public static class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String CSP_STRICT =
        "default-src 'none'; img-src 'self'; style-src 'self';";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http
          .authorizeRequests()
            .antMatchers("/assets/**", "/auth/*").permitAll()
            .anyRequest().authenticated()
            .and()
          .formLogin()
            .loginPage("/auth/login")
            .and()
          .headers()
            .xssProtection().disable()
            .contentSecurityPolicy(CSP_STRICT);
    }
  }
}
