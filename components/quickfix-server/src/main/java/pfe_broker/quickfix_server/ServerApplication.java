package pfe_broker.quickfix_server;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.io.ResourceLoader;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.Order;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.avro.utils.Converters;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DataDictionary;
import quickfix.DoNotSend;
import quickfix.FieldNotFound;
import quickfix.IncorrectDataFormat;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.MessageCracker;
import quickfix.RejectLogon;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.UnsupportedMessageType;
import quickfix.field.AvgPx;
import quickfix.field.CumQty;
import quickfix.field.ExecID;
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
import quickfix.field.Password;
import quickfix.field.SenderCompID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TargetCompID;
import quickfix.field.Username;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logon;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.MarketDataSnapshotFullRefresh;
import quickfix.fix44.NewOrderSingle;

@Singleton
public class ServerApplication extends MessageCracker implements Application {

  private static final Logger LOG = LoggerFactory.getLogger(
    ServerApplication.class
  );

  private Map<String, SessionID> sessionIDMap = new HashMap<>();

  @Inject
  private OrderProducer orderProducer;

  @Inject
  private UserRepository userRepository;

  @Property(name = "quickfix.config.data_dictionary")
  private String dataDictionaryPath;

  @Inject
  ResourceLoader resourceLoader;

  DataDictionary dataDictionary;

  @PostConstruct
  public void init() {
    try {
      dataDictionary =
        new DataDictionary(
          resourceLoader
            .getResourceAsStream("classpath:" + dataDictionaryPath)
            .get()
        );
    } catch (ConfigError configError) {
      configError.printStackTrace();
    }
  }

  private Integer orderKey = 0;
  private Integer executionKey = 0;

  public Integer getExecutionKey() {
    return executionKey;
  }

  public Integer getOrderKey() {
    return orderKey;
  }

  @Override
  public void onCreate(SessionID sessionId) {}

  @Override
  public void onLogon(SessionID sessionId) {}

  @Override
  public void onLogout(SessionID sessionId) {}

  @Override
  public void toAdmin(Message message, SessionID sessionId) {}

  @Override
  public void fromAdmin(Message message, SessionID sessionId)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    try {
      String sender = message
        .getHeader()
        .getString(quickfix.field.SenderCompID.FIELD);
      if (message.isAdmin() && message instanceof Logon) {
        String username = message.getString(Username.FIELD);
        String password = message.getString(Password.FIELD);

        // Check credentials
        if (checkCredentials(username, password)) {
          LOG.debug("Valid credentials for: " + username);
        } else {
          LOG.debug("Logon rejected for: " + username);
          throw new RejectLogon("Invalid username or password");
        }

        sessionIDMap.put(sender, sessionId);
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
    logQuickFixJMessage(message, "Received message");
    crack(message, sessionId);
  }

  public void onMessage(NewOrderSingle message, SessionID sessionID)
    throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
    LOG.debug("Received new Single Order");
    Order avroOrder = new Order(
      message.getHeader().getString(SenderCompID.FIELD),
      message.getString(Symbol.FIELD),
      message.getInt(OrderQty.FIELD),
      Converters.Side.toAvro(message.getSide())
    );
    String key =
      avroOrder.getUsername() +
      ":" +
      message.getString(quickfix.field.ClOrdID.FIELD);

    orderKey++;

    orderProducer.sendOrder(key, avroOrder);
  }

  public void onMessage(MarketDataRequest message, SessionID sessionID) {
    try {
      sendMarketDataSnapshot(message);
    } catch (quickfix.FieldNotFound e) {
      e.printStackTrace();
    }
  }

  public void sendMarketDataSnapshot(MarketDataRequest message)
    throws FieldNotFound {
    MarketDataSnapshotFullRefresh fixMD = createMarketDataSnapshot(message);

    String senderCompId = message.getHeader().getString(SenderCompID.FIELD);
    String targetCompId = message.getHeader().getString(TargetCompID.FIELD);
    fixMD.getHeader().setString(SenderCompID.FIELD, targetCompId);
    fixMD.getHeader().setString(TargetCompID.FIELD, senderCompId);

    sendMessage(message, targetCompId);
  }

  public MarketDataSnapshotFullRefresh createMarketDataSnapshot(
    MarketDataRequest message
  ) throws FieldNotFound {
    MarketDataRequest.NoRelatedSym noRelatedSyms =
      new MarketDataRequest.NoRelatedSym();

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

      MarketDataSnapshotFullRefresh.NoMDEntries noMDEntries =
        new MarketDataSnapshotFullRefresh.NoMDEntries();
      noMDEntries.setChar(MDEntryType.FIELD, '0');
      noMDEntries.setDouble(MDEntryPx.FIELD, symbolPrice);
      noMDEntries.setInt(MDEntrySize.FIELD, symbolVolume);
      fixMD.addGroup(noMDEntries);
    }

    return fixMD;
  }

  public void sendTradeReport(String key, Trade trade) {
    Order order = trade.getOrder();
    String symbol = order.getSymbol().toString();
    String execId = executionKey.toString();
    String clOrdID = key.split(":")[1];
    char side = Converters.Side.charFromAvro(order.getSide());
    int tradeQuantity = trade.getQuantity();
    int baseQuantity = order.getQuantity();
    double price = trade.getPrice();

    ExecutionReport executionReport = new ExecutionReport(
      new OrderID(clOrdID),
      new ExecID(execId),
      new ExecType(ExecType.TRADE),
      new OrdStatus(OrdStatus.FILLED),
      new Side(side),
      new LeavesQty(baseQuantity - tradeQuantity),
      new CumQty(0),
      new AvgPx(price)
    );
    executionReport.set(new Symbol(symbol));
    executionReport.set(new OrderQty(tradeQuantity));

    executionKey++;

    sendMessage(executionReport, order.getUsername().toString());
  }

  public void sendRejectedOrderReport(String key, RejectedOrder rejectedOrder) {
    Order order = rejectedOrder.getOrder();
    String symbol = order.getSymbol().toString();
    String execId = executionKey.toString();
    String clOrdID = key.split(":")[1];
    double leavesQty = 10;
    char side = Converters.Side.charFromAvro(order.getSide());
    int quantity = order.getQuantity();
    OrdRejReason rejectReason = Converters.OrderRejectReason.fromAvro(
      rejectedOrder.getReason()
    );
    ExecutionReport executionReport = new ExecutionReport(
      new OrderID(clOrdID),
      new ExecID(execId),
      new ExecType(ExecType.REJECTED),
      new OrdStatus(OrdStatus.REJECTED),
      new Side(side),
      new LeavesQty(leavesQty),
      new CumQty(0),
      new AvgPx(0)
    );
    executionReport.set(new Symbol(symbol));
    executionReport.set(new OrderQty(quantity));
    executionReport.set(rejectReason);

    executionKey++;

    sendMessage(executionReport, order.getUsername().toString());
  }

  /**
   * for now, this method creates the user for testing purposes, normally it should just check the users credentials
   */
  private boolean checkCredentials(String username, String password) {
    User user;
    User userMatch = userRepository.findByUsername(username).orElse(null);
    if (userMatch == null) {
      user = new User("user1", "password", 1000.0);
      userRepository.save(user);
    } else {
      user = userMatch;
    }
    return user != null && user.getPassword().equals(password);
  }

  protected void sendMessage(Message message, String username) {
    logQuickFixJMessage(message, "Sending message");
    SessionID sessionID = sessionIDMap.get(username);
    try {
      Session.sendToTarget(message, sessionID);
    } catch (SessionNotFound | NullPointerException e) {
      LOG.error("Session not found for user [{}]({})", username, sessionID);
    }
  }

  private void logQuickFixJMessage(Message message, String prefix) {
    List<String> messageParts = Arrays
      .stream(message.toString().split("\u0001"))
      .map(s -> {
        String[] split = s.split("=");
        if (split.length != 2) {
          return s;
        }
        String key = split[0];
        String value = split[1];

        String fieldName = dataDictionary.getFieldName(Integer.parseInt(key));
        String fieldValue = dataDictionary.getValueName(
          Integer.parseInt(key),
          value
        );

        if (fieldName == null) {
          fieldName = key;
        }
        if (fieldValue == null) {
          fieldValue = value;
        }

        return fieldName + "=" + fieldValue;
      })
      .toList();

    String messageString = String.join("|", messageParts);
    LOG.debug("{}: {}", prefix, messageString);
  }
}
