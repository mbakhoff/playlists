package red.sigil.playlists;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import red.sigil.playlists.services.PostgresUserService;
import red.sigil.playlists.services.PropertyService;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.sql.DataSource;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;

public final class SpringConfig {

  @Configuration
  @EnableWebMvc
  @EnableScheduling
  public static class AppConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private PropertyService propertyService;

    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
      registry.jsp("/WEB-INF/", ".jsp");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/assets/**")
          .addResourceLocations("/assets/")
          .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic());
    }

    @Bean
    DataSource dataSource() throws Exception {
      ComboPooledDataSource cpds = new ComboPooledDataSource();
      cpds.setDriverClass(propertyService.getProperty("db-driver"));
      cpds.setJdbcUrl(propertyService.getProperty("db-url"));
      return cpds;
    }
  }

  @EnableWebSecurity
  public static class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String CSP_STRICT = "" +
        "default-src 'none'; " +
        "img-src 'self'; " +
        "style-src 'self'; ";

    @Autowired
    private PostgresUserService postgresUserService;

    public UserDetailsService userDetailsService() {
      return postgresUserService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

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

  public static class AppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext container) {
      ServletRegistration.Dynamic dispatcher = container.addServlet("dispatcher", new DispatcherServlet());
      dispatcher.setLoadOnStartup(1);
      dispatcher.addMapping("/");
    }
  }

  public static class SecurityInitializer extends AbstractSecurityWebApplicationInitializer {

    @Override
    protected void beforeSpringSecurityFilterChain(ServletContext servletContext) {
      super.beforeSpringSecurityFilterChain(servletContext);
      servletContext
          .addFilter("characterEncodingFilter", new CharacterEncodingFilter("UTF-8"))
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
      servletContext
          .addFilter("errorLogger", new ErrorLoggingFilter())
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }
  }
}
