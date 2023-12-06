package pfe_broker.common;

import static pfe_broker.log.Log.LOG;

import io.micronaut.configuration.kafka.streams.event.BeforeKafkaStreamStart;
import io.micronaut.context.event.ApplicationEventListener;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;

public class KafkaStreamUncaughtExceptionHandler
  implements
    ApplicationEventListener<BeforeKafkaStreamStart>,
    StreamsUncaughtExceptionHandler {

  @Override
  public void onApplicationEvent(BeforeKafkaStreamStart event) {
    event.getKafkaStreams().setUncaughtExceptionHandler(this);
  }

  @Override
  public StreamThreadExceptionResponse handle(Throwable exception) {
    LOG.error("Uncaught exception in Kafka Streams", exception);
    return StreamThreadExceptionResponse.REPLACE_THREAD;
  }
}
