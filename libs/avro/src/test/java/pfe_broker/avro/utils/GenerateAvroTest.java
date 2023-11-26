package pfe_broker.avro.utils;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.File;

class GenerateAvroTest {

  @Test
  void testFilesExist() {
    for (File file : GenerateAvro.files) {
      assertTrue(file.exists());
    }
  }

}
