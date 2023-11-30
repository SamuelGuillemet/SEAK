package pfe_broker.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

  /**
   * Logger attached to the `pfe_broker` logger.
   */
  public static final Logger LOG;

  static {
    LOG = LogManager.getLogger("pfe_broker");
  }
}
