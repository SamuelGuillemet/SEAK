package pfe_broker.quickfix_server.interfaces;

import quickfix.Message;
import quickfix.SessionID;

public interface IMessageSender {
  public void registerNewUser(String username, SessionID sessionID);

  public void sendMessage(Message message, String username);
}
