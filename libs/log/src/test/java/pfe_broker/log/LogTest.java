package pfe_broker.log;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogTest {

  @Test
  void testLogInitialization() {
    assertNotNull(Log.LOG, "Log.LOG should not be null");
  }
}
