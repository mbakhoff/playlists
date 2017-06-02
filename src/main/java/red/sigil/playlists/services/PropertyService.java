package red.sigil.playlists.services;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Component
public class PropertyService {

  private Properties props;

  @PostConstruct
  public void init() throws Exception {
    this.props = readProperties();
  }

  public String getProperty(String key) {
    String sys = System.getProperty(key);
    if (sys != null)
      return sys;

    String prop = props.getProperty(key);
    if (prop != null)
      return prop;

    throw new IllegalStateException(key);
  }

  public Properties getProps() {
    return props;
  }

  private Properties readProperties() throws IOException {
    Properties props = new Properties();
    try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
      if (is == null)
        throw new FileNotFoundException("application.properties");
      props.load(is);
    }
    return props;
  }
}
