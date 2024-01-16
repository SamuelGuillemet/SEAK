package pfe_broker.quickfix_server;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.OrderBookRequestType;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.avro.Type;
import pfe_broker.avro.utils.Converters;
import pfe_broker.models.domains.User;
import pfe_broker.models.repositories.UserRepository;
import pfe_broker.quickfix_server.interfaces.IMessageSender;
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
import quickfix.field.AvgPx;
import quickfix.field.ClOrdID;
import quickfix.field.CumQty;
import quickfix.field.CxlRejResponseTo;
import quickfix.field.ExecID;
import quickfix.field.ExecType;
import quickfix.field.LeavesQty;
import quickfix.field.OrdRejReason;
import quickfix.field.OrdStatus;
import quickfix.field.OrdType;
import quickfix.field.OrderID;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Password;
import quickfix.field.SenderCompID;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.Username;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logon;
import quickfix.fix44.MarketDataRequest;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelReject;
import quickfix.fix44.OrderCancelReplaceRequest;
import quickfix.fix44.OrderCancelRequest;

@Singleton
public class ServerApplication extends MessageCracker implements Application {

  private static final Logger LOG = LoggerFactory.getLogger(
    ServerApplication.class
  );

  private final QuickFixLogger quickFixLogger;
  private final IMessageSender messageSender;
  private final OrderProducer orderProducer;
  private final UserRepository userRepository;
  private final MeterRegistry meterRegistry;

  private Integer orderKey;
  private Integer executionKey;

  public ServerApplication(
    QuickFixLogger quickFixLogger,
    IMessageSender messageSender,
    OrderProducer orderProducer,
    UserRepository userRepository,
    MeterRegistry meterRegistry
  ) {
    this.quickFixLogger = quickFixLogger;
    this.messageSender = messageSender;
    this.orderProducer = orderProducer;
    this.userRepository = userRepository;
    this.meterRegistry = meterRegistry;

    this.orderKey = 0;
    this.executionKey = 0;
  }

  /**
   * This method is called when a new session is created.
   *
   * @param sessionId The ID of the newly created session.
   */
  @Override
  public void onCreate(SessionID sessionId) {
    // Nothing to do
  }

  /**
   * Called when a session is successfully logged on.
   *
   * @param sessionId The session ID of the logged on session.
   */
  @Override
  public void onLogon(SessionID sessionId) {
    // Nothing to do
  }

  /**
   * Called when a session is logged out.
   * Unregisters the user associated with the session.
   *
   * @param sessionId The session ID of the logged out session.
   */
  @Override
  public void onLogout(SessionID sessionId) {
    String username = sessionId.getTargetCompID();
    messageSender.unregisterUser(username);
  }

  /**
   * This method is called when a message needs to be sent to the counterparty.
   * It allows the application to modify the message before it is sent.
   *
   * @param message    the message to be sent
   * @param sessionId  the session ID of the counterparty
   */
  @Override
  public void toAdmin(Message message, SessionID sessionId) {
    // Nothing to do
  }

  /**
   * Handles the fromAdmin message received by the server.
   * This method is called when an administrative message is received from the client.
   * It checks the credentials provided in the Logon message and registers the user if the credentials are valid.
   *
   * @param message    The received message.
   * @param sessionId  The session ID of the client.
   * @throws FieldNotFound        If a required field is not found in the message.
   * @throws IncorrectDataFormat  If the data format in the message is incorrect.
   * @throws IncorrectTagValue    If the tag value in the message is incorrect.
   * @throws RejectLogon          If the logon is rejected due to invalid credentials.
   */
  @Override
  public void fromAdmin(Message message, SessionID sessionId)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon {
    if (message.isAdmin() && message instanceof Logon) {
      try {
        String sender = message
          .getHeader()
          .getString(quickfix.field.SenderCompID.FIELD);
        String username = message.getString(Username.FIELD);
        String password = message.getString(Password.FIELD);

        // Check credentials
        if (checkCredentials(username, password)) {
          LOG.debug("Valid credentials for: {}", username);
        } else {
          LOG.debug("Logon rejected for: {}", username);
          throw new RejectLogon("Invalid username or password");
        }
        LOG.info("Logon received from: {}", sender);
        messageSender.registerNewUser(sender, sessionId);
      } catch (FieldNotFound e) {
        e.printStackTrace();
        throw new RejectLogon("Invalid username or password");
      }
    }
  }

  /**
   * This method is called when an outgoing message needs to be sent to the counterparty.
   *
   * @param message    the outgoing message to be sent
   * @param sessionId  the session ID of the counterparty
   * @throws DoNotSend if the message should not be sent
   */
  @Override
  public void toApp(Message message, SessionID sessionId) throws DoNotSend {
    // Nothing to do
  }

  /**
   * This method is called when a message is received from the FIX engine.
   * It logs the received message, increments the counter for the received messages,
   * and delegates the message processing to the crack method.
   *
   * @param message    The received message.
   * @param sessionId  The session ID associated with the message.
   * @throws FieldNotFound          If a required field is not found in the message.
   * @throws IncorrectDataFormat    If the message has an incorrect data format.
   * @throws IncorrectTagValue      If a field in the message has an incorrect value.
   * @throws UnsupportedMessageType If the message type is not supported.
   */
  @Override
  public void fromApp(Message message, SessionID sessionId)
    throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
    quickFixLogger.logQuickFixJMessage(message, "Received message");
    meterRegistry
      .counter(
        "quickfix_server_messages_received",
        "type",
        message.getClass().getSimpleName()
      )
      .increment();
    crack(message, sessionId);
  }

  /**
   * This method is called when a NewOrderSingle message is received
   * @param message
   * @param sessionID
   * @throws FieldNotFound
   * @throws IncorrectTagValue
   */
  public void onMessage(NewOrderSingle message, SessionID sessionID)
    throws FieldNotFound, IncorrectTagValue {
    String username = message.getHeader().getString(SenderCompID.FIELD);
    String symbol = message.getString(Symbol.FIELD);
    int quantity = message.getInt(OrderQty.FIELD);
    pfe_broker.avro.Side side = Converters.Side.toAvro(message.getSide());
    pfe_broker.avro.Type type = Converters.Type.toAvro(message.getOrdType());
    String clOrdID = message.getString(ClOrdID.FIELD);

    Order order;
    switch (type) {
      case MARKET:
        order =
          new Order(username, symbol, quantity, side, type, null, clOrdID);
        break;
      case LIMIT:
        double price = message.getDouble(quickfix.field.Price.FIELD);
        order =
          new Order(username, symbol, quantity, side, type, price, clOrdID);
        break;
      default:
        throw new IncorrectTagValue(quickfix.field.OrdType.FIELD);
    }

    String key = username + ":" + orderKey.toString();

    orderKey++;

    orderProducer.sendOrder(key, order);
  }

  /**
   * This method is called when a OrderCancelReplaceRequest message is received
   * @param message
   * @param sessionID
   * @throws FieldNotFound
   * @throws IncorrectTagValue
   */

  public void onMessage(OrderCancelReplaceRequest message, SessionID sessionID)
    throws FieldNotFound, IncorrectTagValue {
    String username = message.getHeader().getString(SenderCompID.FIELD);
    String symbol = message.getString(Symbol.FIELD);
    int quantity = message.getInt(OrderQty.FIELD);
    pfe_broker.avro.Side side = Converters.Side.toAvro(message.getSide());
    pfe_broker.avro.Type type = Converters.Type.toAvro(message.getOrdType());
    String clOrdID = message.getString(ClOrdID.FIELD);
    String origClOrdID = message.getString(OrigClOrdID.FIELD);
    String orderId = message.getString(OrderID.FIELD);

    if (type != pfe_broker.avro.Type.LIMIT) {
      throw new IncorrectTagValue(quickfix.field.OrdType.FIELD);
    }

    double price = message.getDouble(quickfix.field.Price.FIELD);
    Order order = new Order(
      username,
      symbol,
      quantity,
      side,
      type,
      price,
      clOrdID
    );

    String key = username + ":" + orderId;
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.REPLACE,
      order,
      origClOrdID
    );

    orderProducer.sendOrderBookRequest(key, orderBookRequest);
  }

  /**
   * This method is called when a OrderCancelRequest message is received
   * @param message
   * @param sessionID
   * @throws FieldNotFound
   */
  public void onMessage(OrderCancelRequest message, SessionID sessionID)
    throws FieldNotFound {
    String username = message.getHeader().getString(SenderCompID.FIELD);
    String symbol = message.getString(Symbol.FIELD);
    pfe_broker.avro.Side side = Converters.Side.toAvro(message.getSide());
    String clOrdID = message.getString(ClOrdID.FIELD);
    String origClOrdID = message.getString(OrigClOrdID.FIELD);
    String orderId = message.getString(OrderID.FIELD);

    Order order = new Order(
      username,
      symbol,
      0,
      side,
      Type.LIMIT,
      0.0,
      clOrdID
    );

    String key = username + ":" + orderId;
    OrderBookRequest orderBookRequest = new OrderBookRequest(
      OrderBookRequestType.CANCEL,
      order,
      origClOrdID
    );

    orderProducer.sendOrderBookRequest(key, orderBookRequest);
  }

  /**
   * This method is called when a MarketDataRequest message is received
   * @param message
   * @param sessionID
   */
  public void onMessage(MarketDataRequest message, SessionID sessionID) {
    throw new UnsupportedOperationException(
      "Unimplemented method 'onMessage(MarketDataRequest message, SessionID sessionID)'"
    );
  }

  /**
   * This method is called to send a trade report
   * @param key
   * @param trade
   */
  public void sendTradeReport(String key, Trade trade) {
    Order order = trade.getOrder();
    int tradeQuantity = trade.getQuantity();
    int baseQuantity = order.getQuantity();

    ExecutionReport executionReport = buildExecutionReport(
      key,
      order,
      OrdStatus.FILLED,
      ExecType.TRADE,
      baseQuantity - tradeQuantity,
      tradeQuantity,
      trade.getPrice()
    );

    messageSender.sendMessage(executionReport, order.getUsername().toString());
  }

  /**
   * This method is called to send a rejected order report
   * @param key
   * @param rejectedOrder
   */
  public void sendRejectedOrderReport(String key, RejectedOrder rejectedOrder) {
    Order order = rejectedOrder.getOrder();
    int rejectReason = Converters.OrderRejectReason.intFromAvro(
      rejectedOrder.getReason()
    );

    ExecutionReport executionReport = buildExecutionReport(
      key,
      order,
      OrdStatus.REJECTED,
      ExecType.REJECTED,
      0,
      0,
      0.0
    );
    executionReport.set(new OrdRejReason(rejectReason));

    messageSender.sendMessage(executionReport, order.getUsername().toString());
  }

  /**
   * This method is called to send an order book report
   * @param key
   * @param orderBookRequest
   */
  public void sendOrderBookReport(
    String key,
    OrderBookRequest orderBookRequest
  ) {
    Order order = orderBookRequest.getOrder();
    char execType;
    char ordStatus;

    switch (orderBookRequest.getType()) {
      case NEW:
        execType = ExecType.NEW;
        ordStatus = OrdStatus.NEW;
        break;
      case CANCEL:
        execType = ExecType.CANCELED;
        ordStatus = OrdStatus.CANCELED;
        break;
      case REPLACE:
        execType = ExecType.REPLACED;
        ordStatus = OrdStatus.NEW;
        break;
      default:
        throw new UnsupportedOperationException(
          "Unimplemented OrderBookRequestType"
        );
    }

    ExecutionReport executionReport = buildExecutionReport(
      key,
      order,
      ordStatus,
      execType,
      order.getQuantity(),
      0,
      order.getPrice()
    );

    if (
      orderBookRequest.getType() == OrderBookRequestType.REPLACE ||
      orderBookRequest.getType() == OrderBookRequestType.CANCEL
    ) {
      String origClOrdID = orderBookRequest.getOrigClOrderID().toString();
      executionReport.set(new OrigClOrdID(origClOrdID));
    }

    messageSender.sendMessage(executionReport, order.getUsername().toString());
  }

  /**
   * This method is called to send an order book rejected report
   * @param key
   * @param orderBookRequest
   */
  public void sendOrderBookRejected(
    String key,
    OrderBookRequest orderBookRequest
  ) {
    Order order = orderBookRequest.getOrder();
    String orderID = key.split(":")[1];
    String clOrdID = order.getClOrderID().toString();
    String origClOrdID = orderBookRequest.getOrigClOrderID().toString();
    OrderBookRequestType orderBookRequestType = orderBookRequest.getType();

    char cxlRejResponseTo;
    switch (orderBookRequestType) {
      case CANCEL:
        cxlRejResponseTo = CxlRejResponseTo.ORDER_CANCEL_REQUEST;
        break;
      case REPLACE:
        cxlRejResponseTo = CxlRejResponseTo.ORDER_CANCEL_REPLACE_REQUEST;
        break;
      default:
        throw new UnsupportedOperationException(
          "Unimplemented OrderBookRequestType"
        );
    }

    OrderCancelReject orderCancelReject = new OrderCancelReject(
      new OrderID(orderID),
      new ClOrdID(clOrdID),
      new OrigClOrdID(origClOrdID),
      new OrdStatus(OrdStatus.REJECTED),
      new CxlRejResponseTo(cxlRejResponseTo)
    );

    messageSender.sendMessage(
      orderCancelReject,
      order.getUsername().toString()
    );
  }

  /**
   * This method is called to build an execution report
   * @param key the kafka key
   * @param order the order
   * @param ordStatus
   * @param execType
   * @param leavesQty
   * @param cumQty
   * @param avgPx
   * @return
   */
  private ExecutionReport buildExecutionReport(
    String key,
    Order order,
    char ordStatus,
    char execType,
    int leavesQty,
    int cumQty,
    Double avgPx
  ) {
    String symbol = order.getSymbol().toString();
    String execId = executionKey.toString();
    String orderID = key.split(":")[1];
    char side = Converters.Side.charFromAvro(order.getSide());
    char type = Converters.Type.charFromAvro(order.getType());
    int quantity = order.getQuantity();
    String clOrdID = order.getClOrderID().toString();

    ExecutionReport executionReport = new ExecutionReport(
      new OrderID(orderID),
      new ExecID(execId),
      new ExecType(execType),
      new OrdStatus(ordStatus),
      new Side(side),
      new LeavesQty(leavesQty),
      new CumQty(cumQty),
      new AvgPx(avgPx)
    );
    executionReport.set(new Symbol(symbol));
    executionReport.set(new OrderQty(quantity));
    executionReport.set(new ClOrdID(clOrdID));
    executionReport.set(new OrdType(type));

    executionKey++;

    return executionReport;
  }

  /**
   * for now, this method creates the user for testing purposes, normally it should just check the users credentials
   */
  private boolean checkCredentials(String username, String password) {
    User user;
    User userMatch = userRepository.findByUsername(username).orElse(null);
    if (userMatch == null) {
      user = new User(username, password, 1000.0);
      userRepository.save(user);
    } else {
      user = userMatch;
    }
    return user.getPassword().equals(password);
  }
}
