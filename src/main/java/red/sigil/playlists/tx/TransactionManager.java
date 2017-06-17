package red.sigil.playlists.tx;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

@Component
class TransactionManager {

  private static final ThreadLocal<Connection> current = new ThreadLocal<>();

  private final DataSource dataSource;

  @Autowired
  public TransactionManager(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public TransactionAwareConnection getConnection() {
    return (TransactionAwareConnection) Proxy.newProxyInstance(
        getClass().getClassLoader(),
        new Class[]{TransactionAwareConnection.class},
        this::invokeOnActiveConnection);
  }

  private Object invokeOnActiveConnection(Object proxy, Method method, Object[] args) throws Throwable {
    Connection conn = current.get();
    if (conn == null)
      throw new RuntimeException("no transaction");

    return method.invoke(conn, args);
  }

  void before() throws SQLException {
    Connection conn = current.get();
    if (conn != null)
      throw new RuntimeException("reentrant transactions not implemented");
    conn = dataSource.getConnection();
    conn.setAutoCommit(false);
    current.set(conn);
  }

  void after(Throwable t) throws SQLException {
    Connection conn = current.get();
    current.set(null);
    if (conn == null)
      throw new RuntimeException("no transaction");
    try (Connection c = conn) {
      if (t == null) {
        c.commit();
      } else {
        c.rollback();
      }
    }
  }
}
