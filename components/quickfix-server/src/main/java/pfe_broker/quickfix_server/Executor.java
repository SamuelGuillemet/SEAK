package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.core.io.ResourceLoader;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.Optional;
import javax.management.ObjectName;
import org.quickfixj.jmx.JmxExporter;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionSettings;
import quickfix.SocketAcceptor;

@Singleton
public class Executor implements ApplicationEventListener<StartupEvent> {

  @Property(name = "quickfix.config.executor_dynamic")
  private String config;

  @Inject
  private ResourceLoader resourceLoader;

  @Inject
  private ApplicationMessageCracker applicationMessageCracker;

  private SessionSettings sessionSettings;
  private MessageStoreFactory messageStoreFactory;
  private LogFactory logFactory;
  private MessageFactory messageFactory;
  private SocketAcceptor socketAcceptor;

  private boolean postConstructFailed = true;

  @PostConstruct
  private void postConstruct() {
    Optional<InputStream> configStream = resourceLoader.getResourceAsStream(
      config
    );
    if (!configStream.isPresent()) {
      LOG.error("Could not find config file: {}", config);
      return;
    }
    try {
      sessionSettings = new SessionSettings(configStream.get());
    } catch (ConfigError e) {
      LOG.error("Error loading config file: {}", config, e);
      return;
    }

    messageStoreFactory = new FileStoreFactory(sessionSettings);
    logFactory = new SLF4JLogFactory(sessionSettings);
    messageFactory = new DefaultMessageFactory();

    try {
      socketAcceptor =
        new SocketAcceptor(
          applicationMessageCracker,
          messageStoreFactory,
          sessionSettings,
          logFactory,
          messageFactory
        );
    } catch (ConfigError e) {
      LOG.error("Error creating socket acceptor", e);
      return;
    }

    postConstructFailed = false;
  }

  public void start() {
    try {
      this.configureDynamicSessions();

      JmxExporter jmxExporter = new JmxExporter();
      ObjectName connectorObjectName = jmxExporter.register(socketAcceptor);
      LOG.info("Acceptor registered with JMX, name={}", connectorObjectName);

      socketAcceptor.start();
    } catch (Exception e) {
      LOG.error("Error starting server", e);
    }
  }

  private void configureDynamicSessions() {
    // TODO Configure dynamic sessions
  }

  @Override
  public void onApplicationEvent(StartupEvent event) {
    if (postConstructFailed) {
      LOG.error("Post construct failed, exiting");
      return;
    }
    start();
  }
}
