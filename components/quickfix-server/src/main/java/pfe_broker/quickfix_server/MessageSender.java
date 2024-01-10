package pfe_broker.quickfix_server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
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

  private Map<String, SessionID> sessionIDMap = new HashMap<>();

  @Inject
  private QuickFixLogger quickFixLogger;

  @Override
  public void registerNewUser(String username, SessionID sessionID) {
    sessionIDMap.put(username, sessionID);
  }

  @Override
  public void sendMessage(Message message, String username) {
    quickFixLogger.logQuickFixJMessage(message, "Sending message");
    SessionID sessionID = sessionIDMap.get(username);
    try {
      Session.sendToTarget(message, sessionID);
    } catch (SessionNotFound | NullPointerException e) {
      LOG.error("Session not found for user [{}]({})", username, sessionID);
    }
  }
}
