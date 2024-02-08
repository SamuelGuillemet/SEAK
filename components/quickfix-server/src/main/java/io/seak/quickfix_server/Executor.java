package io.seak.quickfix_server;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Acceptor;

@Context
public class Executor {

  private static final Logger LOG = LoggerFactory.getLogger(Executor.class);

  private final Acceptor serverAcceptor;

  public Executor(Acceptor serverAcceptor) {
    this.serverAcceptor = serverAcceptor;
  }

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
