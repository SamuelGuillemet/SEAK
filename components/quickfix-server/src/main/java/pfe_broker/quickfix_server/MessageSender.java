package pfe_broker.quickfix_server;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.quickfix_server.interfaces.IMessageSender;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;

@Singleton
public class MessageSender implements IMessageSender {

  private static final Logger LOG = LoggerFactory.getLogger(
    MessageSender.class
  );

  private final Map<String, SessionID> sessionIDMap;
  private final Map<String, Queue<Message>> messageQueueMap;
  private final QuickFixLogger quickFixLogger;
  private final MeterRegistry meterRegistry;

  public MessageSender(
    QuickFixLogger quickFixLogger,
    MeterRegistry meterRegistry
  ) {
    this.quickFixLogger = quickFixLogger;
    this.meterRegistry = meterRegistry;
    this.sessionIDMap = new HashMap<>();
    this.messageQueueMap = new HashMap<>();
    meterRegistry.gauge(
      "quickfix_server_users_connected",
      sessionIDMap,
      Map::size
    );
  }

  @Override
  public void registerNewUser(String username, SessionID sessionID) {
    sessionIDMap.put(username, sessionID);
    if (messageQueueMap.containsKey(username)) {
      Queue<Message> messageQueue = messageQueueMap.get(username);
      while (!messageQueue.isEmpty()) {
        Message message = messageQueue.poll();
        sendMessage(message, username);
      }
      messageQueueMap.remove(username);
    }
  }

  @Override
  public void unregisterUser(String username) {
    sessionIDMap.remove(username);
  }

  @Override
  public void sendMessage(Message message, String username) {
    quickFixLogger.logQuickFixJMessage(message, "Sending message");
    meterRegistry
      .counter(
        "quickfix_server_messages_sent",
        "type",
        message.getClass().getSimpleName()
      )
      .increment();
    SessionID sessionID = sessionIDMap.get(username);
    try {
      Session.sendToTarget(message, sessionID);
    } catch (SessionNotFound | NullPointerException e) {
      LOG.error("Session not found for user [{}]({})", username, sessionID);
      messageQueueMap.computeIfAbsent(username, k -> new LinkedList<>());
      messageQueueMap.get(username).add(message);
    }
  }
}
