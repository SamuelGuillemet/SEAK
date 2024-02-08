package io.seak.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Log {

  /**
   * Logger attached to the `seak` logger.
   */
  public static final Logger LOG;

  public static final Logger DB;

  static {
    LOG = LogManager.getLogger("seak");
    DB = LogManager.getLogger("seak.db");
  }
}
