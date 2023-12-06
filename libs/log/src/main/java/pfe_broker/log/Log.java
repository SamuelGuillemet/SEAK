package pfe_broker.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

  /**
   * Logger attached to the `pfe_broker` logger.
   */
  public static final Logger LOG;

  public static final Logger DB;

  static {
    LOG = LogManager.getLogger("pfe_broker");
    DB = LogManager.getLogger("pfe_broker.db");
  }
}
