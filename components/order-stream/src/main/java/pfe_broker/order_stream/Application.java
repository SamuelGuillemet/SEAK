package pfe_broker.order_stream;

import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
  static {
    setProperties();
  }

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);

  public static void main(String[] args) {
    LOG.info("Starting Order Stream");
    Micronaut.run(Application.class, args);
  }

  public static void setProperties() {
    System.setProperty(
      "micronaut.config.files",
      "classpath:application.yml,classpath:kafka.yml,classpath:redis.yml"
    );
  }
}
