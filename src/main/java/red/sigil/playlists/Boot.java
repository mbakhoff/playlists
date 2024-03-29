package red.sigil.playlists;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import red.sigil.playlists.jdbi.InstantArgumentFactory;
import red.sigil.playlists.jdbi.InstantColumnMapper;
import red.sigil.playlists.jdbi.TransactionAwareJdbiAttachment;
import red.sigil.playlists.model.Account;
import red.sigil.playlists.model.Playlist;
import red.sigil.playlists.model.PlaylistItem;
import red.sigil.playlists.services.AccountRepository;
import red.sigil.playlists.services.PlaylistRepository;
import red.sigil.playlists.utils.H2Setup;

import javax.sql.DataSource;
import java.time.Instant;

import static org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN;

@SpringBootApplication
@EnableWebSecurity
@EnableTransactionManagement
@EnableScheduling
public class Boot {

  public static void main(String[] args) {
    SpringApplication.run(Boot.class, args);
  }

  @Bean
  Jdbi jdbi(DataSource ds) throws Exception {
    H2Setup.ensureSchema(ds);
    Jdbi jdbi = Jdbi.create(new TransactionAwareDataSourceProxy(ds));
    jdbi.installPlugin(new SqlObjectPlugin());
    jdbi.registerRowMapper(ConstructorMapper.factory(Account.class));
    jdbi.registerRowMapper(ConstructorMapper.factory(Playlist.class));
    jdbi.registerRowMapper(ConstructorMapper.factory(PlaylistItem.class));
    jdbi.registerColumnMapper(Instant.class, new InstantColumnMapper());
    jdbi.registerArgument(new InstantArgumentFactory());
    return jdbi;
  }

  @Bean
  AccountRepository accountRepository(Jdbi j) {
    return TransactionAwareJdbiAttachment.create(j, AccountRepository.class);
  }

  @Bean
  PlaylistRepository playlistRepository(Jdbi j) {
    return TransactionAwareJdbiAttachment.create(j, PlaylistRepository.class);
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    final String CSP_STRICT = "" +
        "default-src 'none';" +
        "img-src 'self';" +
        "style-src 'self';" +
        "form-action 'self';" +
        "frame-ancestors 'none';";

    http
        .authorizeHttpRequests()
        .requestMatchers("/assets/**", "/auth/*").permitAll()
        .anyRequest().authenticated()
        .and()
        .formLogin()
        .loginPage("/auth/login")
        .and()
        .headers()
        .xssProtection().disable()
        .referrerPolicy(SAME_ORIGIN).and()
        .contentSecurityPolicy(CSP_STRICT);
    return http.build();
  }
}
