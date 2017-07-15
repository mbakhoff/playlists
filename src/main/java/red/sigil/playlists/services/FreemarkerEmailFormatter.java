package red.sigil.playlists.services;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import red.sigil.playlists.ScheduledUpdater.PlaylistChange;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FreemarkerEmailFormatter {

  private final Configuration cfg;

  @Autowired
  public FreemarkerEmailFormatter(Configuration cfg) {
    this.cfg = cfg;
  }

  public String generatePlaylistsChangedNotification(List<PlaylistChange> playlistChanges) throws IOException, TemplateException {
    Map<String, Object> model = new HashMap<>();
    model.put("playlistChanges", playlistChanges);

    StringWriter out = new StringWriter();
    cfg.getTemplate("notification.ftl").process(model, out);
    return out.toString();
  }
}
