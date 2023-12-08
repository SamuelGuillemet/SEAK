package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import quickfix.Acceptor;

@Context
public class Executor {

  @Inject
  private Acceptor serverAcceptor;

  @PostConstruct
  public void start() {
    try {
      serverAcceptor.start();
    } catch (Exception e) {
      LOG.error("Error starting server", e);
    }
  }

  @PreDestroy
  public void stop() {
    try {
      serverAcceptor.stop();
    } catch (Exception e) {
      LOG.error("Error stopping server", e);
    }
  }
}
