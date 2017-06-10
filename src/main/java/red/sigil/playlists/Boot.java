package red.sigil.playlists;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import red.sigil.playlists.services.PropertyService;

import javax.sql.DataSource;

@SpringBootApplication
@EnableWebSecurity
@EnableScheduling
public class Boot {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Boot.class, args);
  }

  @Bean
  DataSource dataSource(PropertyService propertyService) throws Exception {
    ComboPooledDataSource cpds = new ComboPooledDataSource();
    cpds.setDriverClass(propertyService.getProperty("db-driver"));
    cpds.setJdbcUrl(propertyService.getProperty("db-url"));
    return cpds;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
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
