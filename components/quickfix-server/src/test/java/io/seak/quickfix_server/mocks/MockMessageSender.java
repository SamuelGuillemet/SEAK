package io.seak.quickfix_server.mocks;

import io.micronaut.context.annotation.Replaces;
import io.seak.quickfix_server.MessageSender;
import io.seak.quickfix_server.QuickFixLogger;
import io.seak.quickfix_server.interfaces.IMessageSender;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
