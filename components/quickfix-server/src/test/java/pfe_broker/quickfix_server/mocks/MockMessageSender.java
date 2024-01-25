package pfe_broker.quickfix_server.mocks;

import io.micronaut.context.annotation.Replaces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import pfe_broker.quickfix_server.MessageSender;
import pfe_broker.quickfix_server.QuickFixLogger;
import pfe_broker.quickfix_server.interfaces.IMessageSender;
import quickfix.Message;
import quickfix.SessionID;

@Replaces(MessageSender.class)
@Singleton
public class MockMessageSender implements IMessageSender {

  @Inject
  private QuickFixLogger quickFixLogger;

  public BlockingQueue<Message> messages = new LinkedBlockingQueue<>();

  @Override
  public void sendMessage(Message message, String username) {
    quickFixLogger.logQuickFixJMessage(message, "Sending message");
    messages.add(message);
  }

  @Override
  public void registerNewUser(String username, SessionID sessionID) {}

  @Override
  public void unregisterUser(String username) {}
}
