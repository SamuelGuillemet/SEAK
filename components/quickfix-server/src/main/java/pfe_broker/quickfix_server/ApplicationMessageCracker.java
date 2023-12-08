package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
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
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;

@Singleton
public class ApplicationMessageCracker
  extends MessageCracker
  implements Application {

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
  public void fromAdmin(Message message, SessionID sessionId) {}

  @Override
  public void toApp(Message message, SessionID sessionId) throws DoNotSend {}

  @Override
  public void fromApp(Message message, SessionID sessionId)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    LOG.debug("Received message: {}", message);
    crack(message, sessionId);
  }

  public void sendExecutionReport(String key, Trade trade) {
    // TODO
  }

  public void sendOrderCancelReject(String key, RejectedOrder rejectedOrder) {
    // TODO
  }
}
