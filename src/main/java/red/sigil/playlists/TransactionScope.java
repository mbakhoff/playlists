package red.sigil.playlists;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class TransactionScope implements AutoCloseable {

  private static final ThreadLocal<TransactionScope> current = new ThreadLocal<>();

  private final Connection connection;
  private final boolean isRoot;

  public static Connection conn() {
    TransactionScope tlts = current.get();
    if (tlts == null)
      throw new IllegalStateException("no active scope");
    return tlts.connection;
  }

  public TransactionScope(DataSource dataSource) throws SQLException {
    TransactionScope tlts = current.get();
    if (tlts == null) {
      isRoot = true;
      connection = dataSource.getConnection();
      connection.setAutoCommit(false);
      current.set(this);
    } else {
      isRoot = false;
      connection = null;
    }
  }

  public void commit() throws SQLException {
    if (!isRoot)
      return;

    connection.commit();
  }

  @Override
  public void close() throws Exception {
    if (!isRoot)
      return;

    current.set(null);
    if (connection.isClosed())
      return;

    try {
      connection.rollback();
      connection.setAutoCommit(true);
    } finally {
      connection.close();
    }
  }
}
