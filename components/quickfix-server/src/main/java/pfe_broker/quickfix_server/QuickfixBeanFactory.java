package pfe_broker.quickfix_server;

import static quickfix.Acceptor.SETTING_ACCEPTOR_TEMPLATE;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_ADDRESS;
import static quickfix.Acceptor.SETTING_SOCKET_ACCEPT_PORT;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.io.ResourceLoader;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.management.JMException;
import javax.management.ObjectName;
import org.quickfixj.jmx.JmxExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickfix.Acceptor;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SLF4JLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.ThreadedSocketAcceptor;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider;
import quickfix.mina.acceptor.DynamicAcceptorSessionProvider.TemplateMapping;

@Factory
public class QuickfixBeanFactory {

  private static final Logger LOG = LoggerFactory.getLogger(
    QuickfixBeanFactory.class
  );

  @Bean
  @Singleton
  public SessionSettings serverSessionSettings(
    @Property(name = "quickfix.config.executor_dynamic") String config,
    ResourceLoader resourceLoader
  ) {
    Optional<InputStream> configStream = resourceLoader.getResourceAsStream(
      config
    );
    if (!configStream.isPresent()) {
      LOG.error("Could not find config file: {}", config);
      return null;
    }
    try {
      return new SessionSettings(configStream.get());
    } catch (ConfigError e) {
      LOG.error("Error loading config file: {}", config, e);
      return null;
    }
  }

  @Bean
  @Singleton
  public Application serverApplication(ServerApplication serverApplication) {
    return serverApplication;
  }

  @Bean
  @Singleton
  public Acceptor serverAcceptor(
    Application serverApplication,
    MessageStoreFactory serverMessageStoreFactory,
    SessionSettings serverSessionSettings,
    LogFactory serverLogFactory,
    MessageFactory serverMessageFactory
  ) throws ConfigError, FieldConvertError, JMException {
    ThreadedSocketAcceptor socketAcceptor = new ThreadedSocketAcceptor(
      serverApplication,
      serverMessageStoreFactory,
      serverSessionSettings,
      serverLogFactory,
      serverMessageFactory
    );

    Map<InetSocketAddress, List<TemplateMapping>> dynamicSessionMappings =
      new HashMap<>();

    Iterator<SessionID> sectionIterator =
      serverSessionSettings.sectionIterator();

    while (sectionIterator.hasNext()) {
      SessionID sessionID = sectionIterator.next();

      if (isSessionTemplate(serverSessionSettings, sessionID)) {
        InetSocketAddress address = getAcceptorSocketAddress(
          serverSessionSettings,
          sessionID
        );
        TemplateMapping templateMapping = new TemplateMapping(
          sessionID,
          sessionID
        );

        if (dynamicSessionMappings.containsKey(address)) {
          dynamicSessionMappings.get(address).add(templateMapping);
        } else {
          List<TemplateMapping> templateMappings = new ArrayList<>();
          templateMappings.add(templateMapping);
          dynamicSessionMappings.put(address, templateMappings);
        }
      }
    }

    dynamicSessionMappings.forEach((key, value) ->
      socketAcceptor.setSessionProvider(
        key,
        new DynamicAcceptorSessionProvider(
          serverSessionSettings,
          value,
          serverApplication,
          serverMessageStoreFactory,
          serverLogFactory,
          serverMessageFactory
        )
      )
    );

    JmxExporter jmxExporter = new JmxExporter();
    ObjectName connectorObjectName = jmxExporter.register(socketAcceptor);
    LOG.info("Acceptor registered with JMX, name={}", connectorObjectName);

    return socketAcceptor;
  }

  @Bean
  @Singleton
  public MessageStoreFactory serverMessageStoreFactory(
    SessionSettings serverSessionSettings
  ) {
    return new FileStoreFactory(serverSessionSettings);
  }

  @Bean
  @Singleton
  public LogFactory serverLogFactory(SessionSettings serverSessionSettings) {
    return new SLF4JLogFactory(serverSessionSettings);
  }

  @Bean
  @Singleton
  public MessageFactory serverMessageFactory() {
    return new DefaultMessageFactory();
  }

  private InetSocketAddress getAcceptorSocketAddress(
    SessionSettings settings,
    SessionID sessionID
  ) throws ConfigError, FieldConvertError {
    String acceptorHost = "0.0.0.0";
    if (settings.isSetting(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS)) {
      acceptorHost =
        settings.getString(sessionID, SETTING_SOCKET_ACCEPT_ADDRESS);
    }
    int acceptorPort = (int) settings.getLong(
      sessionID,
      SETTING_SOCKET_ACCEPT_PORT
    );

    return new InetSocketAddress(acceptorHost, acceptorPort);
  }

  private boolean isSessionTemplate(
    SessionSettings settings,
    SessionID sessionID
  ) throws ConfigError, FieldConvertError {
    return (
      settings.isSetting(sessionID, SETTING_ACCEPTOR_TEMPLATE) &&
      settings.getBool(sessionID, SETTING_ACCEPTOR_TEMPLATE)
    );
  }
}
