package pfe_broker.market_matcher;

import static pfe_broker.log.Log.LOG;

import io.micronaut.runtime.Micronaut;

public class Application {

  public static void main(String[] args) {
    LOG.info("Starting Market Matcher");
    setProperties();
    Micronaut.run(Application.class, args);
  }

  public static void setProperties() {
    System.setProperty(
      "micronaut.config.files",
      "classpath:application.yml,classpath:kafka.yml"
    );
  }
}
