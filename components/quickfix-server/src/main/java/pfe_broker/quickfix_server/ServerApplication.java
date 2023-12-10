package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pfe_broker.avro.Order;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import quickfix.Application;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.SessionID;
import quickfix.UnsupportedMessageType;
import quickfix.field.OrderQty;
import quickfix.field.SenderCompID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.fix42.NewOrderSingle;

@Singleton
public class ServerApplication
  extends MessageCracker
  implements Application {

  @SuppressWarnings("unused")
  @Inject
  private OrderProducer orderProducer;

  @SuppressWarnings("unused")
  @Inject
  private UserRepository userRepository;

  private Integer orderKey = 0;

  @Override
  public void onCreate(SessionID sessionId) {}

  @Override
  public void onLogon(SessionID sessionId) {}

  @Override
  public void onLogout(SessionID sessionId) {}

  @Override
  public void toAdmin(Message message, SessionID sessionId) {}

  @Override
  public void fromAdmin(Message message, SessionID sessionId)  throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    String username = message.getString(553);
    String password = message.getString(554);
    
    // Check credentials
    if (checkCredentials(username, password)) {
      System.out.println("Valid credentials for: " + username);
    } else {
      System.out.println("Logon rejected for: " + username);
      throw new RejectLogon("Invalid username or password");
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

  public void onMessage(NewOrderSingle message, SessionID sessionID) throws FieldNotFound,
            UnsupportedMessageType, IncorrectTagValue {
        try {
            System.out.println("Received new Single Order");
            pfe_broker.avro.Side side;
            if (message.getString(Side.FIELD).charAt(0) == quickfix.field.Side.BUY) {
                side = pfe_broker.avro.Side.BUY;
            } else {
                side = pfe_broker.avro.Side.SELL;
            }
            Order avroOrder = new Order(message.getHeader().getString(SenderCompID.FIELD),
                    message.getString(Symbol.FIELD), message.getInt(OrderQty.FIELD), side);
            orderProducer.sendOrder(orderKey.toString(),avroOrder);
            orderKey++;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

  public void sendExecutionReport(String key, Trade trade) {
    // TODO
  }

  public void sendOrderCancelReject(String key, RejectedOrder rejectedOrder) {
    // TODO
  }

  private boolean checkCredentials(String username, String password) {
    User user = userRepository.findByUsername(username).orElse(null);
    return user != null && user.getPassword().equals(password);
  }

}
