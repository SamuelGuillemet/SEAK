package pfe_broker.avro.utils;

import java.io.File;
import java.io.IOException;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompiler;

public class GenerateAvro {

  // The order of the files is important
  private static final File[] files = new File[] {
      getFileFromRessource("market-data-entry.avsc"),
      getFileFromRessource("market-data-subscription-request.avsc"),
      getFileFromRessource("market-data-rejected-reason.avsc"),
      getFileFromRessource("order-rejected-reason.avsc"),
      getFileFromRessource("type.avsc"),
      getFileFromRessource("side.avsc"),
      getFileFromRessource("order-book-request-type.avsc"),
      getFileFromRessource("order.avsc"),
      getFileFromRessource("trade.avsc"),
      getFileFromRessource("rejected-order.avsc"),
      getFileFromRessource("market-data.avsc"),
      getFileFromRessource("order-book-request.avsc"),
      getFileFromRessource("market-data-request.avsc"),
      getFileFromRessource("market-data-response.avsc"),
      getFileFromRessource("market-data-rejected.avsc"),
  };

  public static File[] getFiles() {
    return files;
  }

  public static void main(String[] args) {
    String outputDir = "src/main/java";

    try {
      compileSchema(files, new File(outputDir));
    } catch (IOException e) {
      System.err.println("Error generating Avro schema");
      e.printStackTrace();
    }
  }

  public static void compileSchema(File[] srcFiles, File dest)
    throws IOException {
    Schema.Parser parser = new Schema.Parser();

    for (File src : srcFiles) {
      Schema schema = parser.parse(src);
      SpecificCompiler compiler = new SpecificCompiler(schema);
      compiler.compileToDestination(src, dest);
      System.out.println("Generated class for schema " + src.getName());
    }
  }

  private static File getFileFromRessource(String filename) {
    ClassLoader classLoader = GenerateAvro.class.getClassLoader();
    return new File(classLoader.getResource(filename).getFile());
  }
}
