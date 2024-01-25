package pfe_broker.quickfix_server;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.Message;

@Singleton
public class QuickFixLogger {

  private static final Logger LOG = LoggerFactory.getLogger(
    QuickFixLogger.class
  );

  private final DataDictionary dataDictionary;

  public QuickFixLogger(
    ResourceLoader resourceLoader,
    @Property(
      name = "quickfix.config.data_dictionary"
    ) String dataDictionaryPath
  ) throws ConfigError {
    dataDictionary =
      new DataDictionary(
        resourceLoader
          .getResourceAsStream("classpath:" + dataDictionaryPath)
          .orElseThrow()
      );
  }

  public void logQuickFixJMessage(Message message, String prefix) {
    List<String> messageParts = Arrays
      .stream(message.toString().split("\u0001"))
      .map(s -> {
        String[] split = s.split("=");
        if (split.length != 2) {
          return s;
        }
        String key = split[0];
        String value = split[1];

        String fieldName = dataDictionary.getFieldName(Integer.parseInt(key));
        String fieldValue = dataDictionary.getValueName(
          Integer.parseInt(key),
          value
        );

        if (fieldName == null) {
          fieldName = key;
        }
        if (fieldValue == null) {
          fieldValue = value;
        }

        return fieldName + "=" + fieldValue;
      })
      .toList();

    String messageString = String.join("|", messageParts);
    LOG.debug("{}: {}", prefix, messageString);
  }
}
