package pfe_broker.trade_stream;

import static pfe_broker.log.Log.LOG;

import io.micronaut.runtime.Micronaut;

public class Application {
  static {
    setProperties();
  }

  public static void main(String[] args) {
    LOG.info("Starting Trade Stream");
    Micronaut.run(Application.class, args);
  }

  public static void setProperties() {
    System.setProperty(
      "micronaut.config.files",
      "classpath:application.yml,classpath:kafka.yml,classpath:redis.yml"
    );
  }
}
