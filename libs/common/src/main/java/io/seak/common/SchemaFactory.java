package io.seak.common;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.seak.avro.utils.SchemaRecord;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Context
public class SchemaFactory {

  private static final Logger LOG = LoggerFactory.getLogger(
    SchemaFactory.class
  );

  SchemaFactory(
    @NonNull List<SchemaRecord> schemas,
    ApplicationContext applicationContext,
    @Property(name = "kafka.schema.registry.url") String schemaRegistryUrl
  ) {
    if (schemas.isEmpty()) {
      return;
    }
    if (!UtilsRunning.isSchemaRegistryRunning(schemaRegistryUrl)) {
      LOG.error("Schema registry is not running");
      return;
    }
    HttpClient httpClient = applicationContext.createBean(
      HttpClient.class,
      schemaRegistryUrl
    );
    LOG.debug("Registering schemas");
    schemas.forEach(schemaRecord -> {
      String schemaString = schemaRecord
        .schema()
        .toString()
        .replace("\"", "\\\"");
      String subjectName = schemaRecord.topicName() + "-value";

      String contentType = "application/vnd.schemaregistry.v1+json";

      // Verify if the schema already exists and is the same
      HttpRequest<String> requestGet = HttpRequest.GET(
        "/subjects/" + subjectName + "/versions/latest"
      );

      try {
        String response = httpClient.toBlocking().retrieve(requestGet);
        if (response.contains(schemaString)) {
          LOG.trace("Schema already registered with latest version");
          return;
        }
      } catch (Exception e) {
        // Do nothing
      }

      HttpRequest<String> request = HttpRequest
        .POST(
          "/subjects/" + subjectName + "/versions",
          "{\"schema\": \"" + schemaString + "\"}"
        )
        .header("Content-Type", contentType);

      try {
        httpClient.toBlocking().retrieve(request);
      } catch (Exception e) {
        LOG.error("Error while registering schema: ", e);
      }
    });

    httpClient.close();
  }
}
