package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import io.micronaut.runtime.Micronaut;

public class MainApplication {
  static {
    setProperties();
  }

  public static void main(String[] args) {
    LOG.info("Starting QuickFIX/J server");
    Micronaut.run(MainApplication.class, args);
  }

  public static void setProperties() {
    System.setProperty(
      "micronaut.config.files",
      "classpath:application.yml,classpath:kafka.yml,classpath:quickfix.yml"
    );
  }
}
