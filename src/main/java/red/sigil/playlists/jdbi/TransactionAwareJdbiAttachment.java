package red.sigil.playlists.jdbi;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class TransactionAwareJdbiAttachment implements InvocationHandler {

  private static final Logger log = LoggerFactory.getLogger(TransactionAwareJdbiAttachment.class);

  private final Jdbi jdbi;
  private final Class<?> attachmentType;

  public TransactionAwareJdbiAttachment(Jdbi jdbi, Class<?> attachmentType) {
    this.jdbi = jdbi;
    this.attachmentType = attachmentType;
  }

  public static <T> T create(Jdbi jdbi, Class<T> attachmentType) {
    return attachmentType.cast(Proxy.newProxyInstance(
        attachmentType.getClassLoader(),
        new Class[]{attachmentType},
        new TransactionAwareJdbiAttachment(jdbi, attachmentType)));
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    if (!TransactionSynchronizationManager.isActualTransactionActive())
      throw new IllegalStateException("no transaction");

    Object attachment = getHolder().getOrCreate(attachmentType);
    return method.invoke(attachment, args);
  }

  private JdbiHolder getHolder() {
    synchronized (jdbi) {
      JdbiHolder holder = (JdbiHolder) TransactionSynchronizationManager.getResource(JdbiHolder.class);
      if (holder == null) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            JdbiHolder bound = (JdbiHolder) TransactionSynchronizationManager.unbindResource(JdbiHolder.class);
            if (bound != null) {
              log.debug("unbound afterCompletion " + bound);
              bound.handle.close();
            }
          }
        });
        holder = new JdbiHolder(jdbi.open());
        TransactionSynchronizationManager.bindResource(JdbiHolder.class, holder);
        log.debug("bound to transaction " + holder);
      }
      return holder;
    }
  }

  static class JdbiHolder {

    final Handle handle;
    final Map<Class<?>, Object> attachements = new HashMap<>();

    JdbiHolder(Handle handle) {
      this.handle = handle;
    }

    synchronized Object getOrCreate(Class<?> attachmentType) {
      Object attachment = attachements.get(attachmentType);
      if (attachment == null) {
        attachment = handle.attach(attachmentType);
        attachements.put(attachmentType, attachment);
      }
      return attachment;
    }
  }
}
