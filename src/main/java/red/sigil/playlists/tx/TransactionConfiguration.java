package red.sigil.playlists.tx;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import red.sigil.playlists.services.PropertyService;

import javax.sql.DataSource;

@Configuration
public class TransactionConfiguration {

  @Bean
  DataSource dataSource(PropertyService propertyService) throws Exception {
    ComboPooledDataSource cpds = new ComboPooledDataSource();
    cpds.setDriverClass(propertyService.getProperty("db-driver"));
    cpds.setJdbcUrl(propertyService.getProperty("db-url"));
    return cpds;
  }

  @Bean
  DefaultAdvisorAutoProxyCreator autoProxyCreator() {
    return new DefaultAdvisorAutoProxyCreator();
  }

  @Bean
  TransactionInterceptor transactionInterceptor() {
    return new TransactionInterceptor();
  }

  @Bean
  TransactionManager transactionManager() {
    return new TransactionManager();
  }

  @Bean
  TransactionAwareConnection txConnection(TransactionManager manager) {
    return manager.getConnection();
  }
}
