package pfe_broker.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

  public static final Logger LOG;

  static {
    System.setProperty("log4j.configurationFile", "shared/log4j2.yml");
    LOG = LogManager.getLogger("pfe_broker");
  }
}
