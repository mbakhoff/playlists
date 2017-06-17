package red.sigil.playlists.tx;

import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
class TransactionInterceptor implements PointcutAdvisor {

  private final TransactionManager transactionManager;

  @Autowired
  public TransactionInterceptor(TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
  }

  @Override
  public boolean isPerInstance() {
    return false;
  }

  @Override
  public Pointcut getPointcut() {
    return new StaticMethodMatcherPointcut() {
      @Override
      public boolean matches(Method method, Class<?> targetClass) {
        return method.getAnnotation(Transactional.class) != null
            || method.getDeclaringClass().getAnnotation(Transactional.class) != null;
      }
    };
  }

  @Override
  public Advice getAdvice() {
    return (MethodInterceptor) invocation -> {
      transactionManager.before();
      try {
        Object result = invocation.proceed();
        transactionManager.after(null);
        return result;
      } catch (Throwable t) {
        transactionManager.after(t);
        throw t;
      }
    };
  }
}
