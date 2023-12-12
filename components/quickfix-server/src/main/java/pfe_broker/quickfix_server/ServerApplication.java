package pfe_broker.quickfix_server;

import static pfe_broker.log.Log.LOG;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.avro.utils.Converters;
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
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
import quickfix.field.ExecTransType;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.MDEntryPx;
import quickfix.field.MDEntrySize;
import quickfix.field.MDEntryType;
import quickfix.field.MDReqID;
import quickfix.field.NoRelatedSym;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.SenderCompID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.MarketDataRequest;
import quickfix.fix42.MarketDataSnapshotFullRefresh;
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
  public void onMessage(MarketDataRequest message, SessionID sessionID) {
    try {
        sendMarketDataSnapshot(message);
    } catch (quickfix.FieldNotFound e) {
        e.printStackTrace();
    }
  }
  public void sendMarketDataSnapshot(MarketDataRequest message) throws FieldNotFound {

    MarketDataSnapshotFullRefresh fixMD = createMarketDataSnapshot(message);

    String senderCompId = message.getHeader().getString(SenderCompID.FIELD);
    String targetCompId = message.getHeader().getString(TargetCompID.FIELD);
    fixMD.getHeader().setString(SenderCompID.FIELD, targetCompId);
    fixMD.getHeader().setString(TargetCompID.FIELD, senderCompId);

    try {
        quickfix.Session.sendToTarget(fixMD, targetCompId, senderCompId);
    } catch (SessionNotFound e) {
        e.printStackTrace();
    }
}

public MarketDataSnapshotFullRefresh createMarketDataSnapshot(MarketDataRequest message) throws FieldNotFound {
    MarketDataRequest.NoRelatedSym noRelatedSyms = new MarketDataRequest.NoRelatedSym();

    int relatedSymbolCount = message.getInt(NoRelatedSym.FIELD);

    MarketDataSnapshotFullRefresh fixMD = new MarketDataSnapshotFullRefresh();
    fixMD.setString(MDReqID.FIELD, message.getString(MDReqID.FIELD));

    for (int i = 1; i <= relatedSymbolCount; ++i) {
        message.getGroup(i, noRelatedSyms);
        String symbol = noRelatedSyms.getString(Symbol.FIELD);
        fixMD.setString(Symbol.FIELD, symbol);

        double symbolPrice = 0.0;
        int symbolVolume = 0;

        if (symbol.equals("GOOGL")) {
            symbolPrice = 123.45;
            symbolVolume = 1000;
        } else if (symbol.equals("AAPL")) {
            symbolPrice = 456.78;
            symbolVolume = 1000;
        }

        MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries = new MarketDataSnapshotFullRefresh.NoMDEntries();
        noMDEntries.setChar(MDEntryType.FIELD, '0');
        noMDEntries.setDouble(MDEntryPx.FIELD, symbolPrice);
        noMDEntries.setInt(MDEntrySize.FIELD, symbolVolume);
        fixMD.addGroup(noMDEntries);
    }

    return fixMD;
}


  public void sendTradeReport(String key, Trade trade) {
    try {
      Order order = trade.getOrder();
      String symbol = order.getSymbol().toString();
      String execId = "execId";
      String clOrdID = "clOrdID";
      pfe_broker.avro.Side sideAvro = order.getSide();
      char side = quickfix.field.Side.SELL;
      switch (sideAvro) {
          case BUY:
              side = quickfix.field.Side.BUY;
              break;
          case SELL:
              side = quickfix.field.Side.SELL;
      }
      int tradeQuantity = trade.getQuantity();
      int baseQuantity = order.getQuantity();
      double price = trade.getPrice();

      ExecutionReport executionReport = new ExecutionReport(
              new OrderID(clOrdID),
              new ExecID(execId),
              new ExecTransType(ExecTransType.NEW),
              new ExecType(ExecType.NEW),
              new OrdStatus(OrdStatus.NEW),
              new Symbol(symbol),
              new Side(side),
              new LeavesQty(baseQuantity - tradeQuantity),
              new CumQty(0),
              new AvgPx(price));
      executionReport.set(new OrderQty(tradeQuantity));
      String senderCompID = "SERVER";
      String targetCompID = "user1";

      SessionID sessionID = new SessionID("FIX.4.2", senderCompID, targetCompID);
      
      sendExecutionReport(executionReport, sessionID);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void sendRejectedOrderReport(String key, RejectedOrder rejectedOrder) {
    try {
      Order order = rejectedOrder.getOrder();
      String symbol = order.getSymbol().toString();
      String execId = "execId";
      String clOrdID = "clOrdID";
      double leavesQty = 10;
      pfe_broker.avro.Side sideAvro = order.getSide();
      char side = Side.SELL;
      switch (sideAvro) {
          case BUY:
              side = Side.BUY;
              break;
          case SELL:
        side = Side.SELL;
      }
      int quantity = order.getQuantity();
      int rejectReason = Converters.OrderRejectReason.charFromAvro(rejectedOrder.getReason());
      ExecutionReport executionReport = new ExecutionReport(
        new OrderID(clOrdID),
        new ExecID(execId),
        new ExecTransType(ExecTransType.CANCEL),
        new ExecType(ExecType.REJECTED),
        new OrdStatus(OrdStatus.REJECTED),
        new Symbol(symbol),
        new Side(side),
        new LeavesQty(leavesQty),
        new CumQty(0),
        new AvgPx(0));
      executionReport.set(new OrderQty(quantity));
      executionReport.setInt(OrdRejReason.FIELD, rejectReason);

      String senderCompID = "SERVER";
      String targetCompID = "user1";

      SessionID sessionID = new SessionID("FIX.4.2", senderCompID, targetCompID);
      sendExecutionReport(executionReport, sessionID);

    } catch (Exception e) {
        e.printStackTrace();
    }
  }

  public void sendExecutionReport(ExecutionReport executionReport, SessionID sessionID) throws SessionNotFound{
    quickfix.Session.sendToTarget(executionReport, sessionID);
  }
  /* for now, this method creates the user for testing purposes, normally it should just check the users credentials
  * 
  */
  private boolean checkCredentials(String username, String password) {
    User user;
    User userMatch = userRepository.findByUsername(username).orElse(null);
    if (userMatch==null) {
      user = new User("user1", "password", 1000.0);
      userRepository.save(user);
    } else {
      user = userMatch;
    }
    return user != null && user.getPassword().equals(password);
  }

}
