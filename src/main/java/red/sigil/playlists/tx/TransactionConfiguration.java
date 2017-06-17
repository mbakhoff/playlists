package red.sigil.playlists.tx;

import org.springframework.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionConfiguration {

  @Bean
  DefaultAdvisorAutoProxyCreator autoProxyCreator() {
    return new DefaultAdvisorAutoProxyCreator();
  }

  @Bean
  TransactionAwareConnection txConnection(TransactionManager manager) {
    return manager.getConnection();
  }
}
