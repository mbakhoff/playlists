package red.sigil.playlists.jdbi;

import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;

public class InstantColumnMapper implements ColumnMapper<Instant> {

  @Override
  public Instant map(ResultSet r, int column, StatementContext ctx) throws SQLException {
    OffsetDateTime odt = r.getObject(column, OffsetDateTime.class);
    return odt != null ? odt.toInstant() : null;
  }

  @Override
  public Instant map(ResultSet r, String column, StatementContext ctx) throws SQLException {
    OffsetDateTime odt = r.getObject(column, OffsetDateTime.class);
    return odt != null ? odt.toInstant() : null;
  }
}
