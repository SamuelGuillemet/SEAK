package pfe_broker.avro.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.Test;

class GenerateAvroTest {

  @Test
  void testFilesExist() {
    for (File file : GenerateAvro.files) {
      assertTrue(file.exists());
    }
  }
}
