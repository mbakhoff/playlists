package red.sigil.playlists.utils;

import org.h2.tools.RunScript;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.sigil.playlists.services.PlaylistService;

import javax.sql.DataSource;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class H2Setup {

  private static Logger log = LoggerFactory.getLogger(PlaylistService.class);

  public static void ensureSchema(DataSource ds) throws Exception {
    Jdbi.create(ds).useTransaction(h -> {
      if (!getTables(h).contains("PLAYLIST")) {
        log.warn("recreating schema");
        try (InputStream is = H2Setup.class.getResourceAsStream("/schema.sql")) {
          if (is == null)
            throw new FileNotFoundException("schema.sql");
          RunScript.execute(h.getConnection(), new InputStreamReader(is, StandardCharsets.UTF_8));
        }
      }
    });
  }

  private static List<String> getTables(Handle h) {
    return h.createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'")
        .mapTo(String.class)
        .list();
  }
}
