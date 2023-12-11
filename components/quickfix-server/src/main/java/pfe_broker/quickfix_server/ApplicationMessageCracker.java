package pfe_broker.quickfix_server;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.models.repositories.UserRepository;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;

@Singleton
public class ApplicationMessageCracker
  extends MessageCracker
  implements Application {

  private static final Logger LOG = LoggerFactory.getLogger(
    ApplicationMessageCracker.class
  );

  Map<String, SessionID> sessionIDMap = new HashMap<>();

  @SuppressWarnings("unused")
  @Inject
  private OrderProducer orderProducer;

  @SuppressWarnings("unused")
  @Inject
  private UserRepository userRepository;

  @Override
  public void onCreate(SessionID sessionId) {}

  @Override
  public void onLogon(SessionID sessionId) {}

  @Override
  public void onLogout(SessionID sessionId) {}

  @Override
  public void toAdmin(Message message, SessionID sessionId) {}

  @Override
  public void fromAdmin(Message message, SessionID sessionId) {
    try {
      String username = message
        .getHeader()
        .getString(quickfix.field.SenderCompID.FIELD);
      if (message.isAdmin() && message instanceof quickfix.fix42.Logon) {
        sessionIDMap.put(username, sessionId);
      }
    } catch (FieldNotFound e) {
      e.printStackTrace();
    }
  }

  @Override
  public void toApp(Message message, SessionID sessionId) throws DoNotSend {}

  @Override
  public void fromApp(Message message, SessionID sessionId)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    LOG.debug("Received message: {}", message);
    crack(message, sessionId);
  }

  @SuppressWarnings("unused")
  private void sendMessage(Message message, String username) {
    SessionID sessionID = sessionIDMap.get(username);
    try {
      Session.sendToTarget(message, sessionID);
    } catch (SessionNotFound sessionNotFound) {
      LOG.error("Session not found for user {}", sessionID);
    }
  }

  @SuppressWarnings("unused")
  private void buildKafkaKey(Message message) throws FieldNotFound {
    String key =
      message.getString(quickfix.field.ClOrdID.FIELD) +
      ":" +
      message.getHeader().getString(quickfix.field.SenderCompID.FIELD);
  }

  public void sendExecutionReport(String key, Trade trade) {
    // TODO
  }

  public void sendOrderCancelReject(String key, RejectedOrder rejectedOrder) {
    // TODO
  }
}
