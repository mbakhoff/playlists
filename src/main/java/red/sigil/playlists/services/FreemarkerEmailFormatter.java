package red.sigil.playlists.services;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapperBuilder;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
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

  private Configuration cfg;

  @PostConstruct
  public void init() {
    Version version = Configuration.VERSION_2_3_26;

    DefaultObjectWrapperBuilder wrapperBuilder = new DefaultObjectWrapperBuilder(version);
    wrapperBuilder.setExposeFields(true);

    Configuration cfg = new Configuration(version);
    cfg.setTemplateLoader(new ClassTemplateLoader(FreemarkerEmailFormatter.class.getClassLoader(), "/templates/"));
    cfg.setDefaultEncoding("UTF-8");
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    cfg.setLogTemplateExceptions(false);
    cfg.setObjectWrapper(wrapperBuilder.build());
    this.cfg = cfg;
  }

  public String generateNotificationMessage(List<PlaylistChange> playlistChanges) throws IOException, TemplateException {
    Map<String, Object> root = new HashMap<>();
    root.put("playlistChanges", playlistChanges);

    StringWriter out = new StringWriter();
    cfg.getTemplate("notification.ftl").process(root, out);
    return out.toString();
  }
}
