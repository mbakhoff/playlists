package red.sigil.playlists.jdbi;

import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.argument.ArgumentFactory;
import org.jdbi.v3.core.config.ConfigRegistry;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

public class InstantArgumentFactory implements ArgumentFactory {

  @Override
  public Optional<Argument> build(Type type, Object value, ConfigRegistry config) {
    if (!Instant.class.equals(type))
      return Optional.empty();

    OffsetDateTime odt = value != null ? ((Instant) value).atOffset(ZoneOffset.UTC) : null;
    return Optional.of((position, statement, ctx) -> statement.setObject(position, odt));
  }
}
