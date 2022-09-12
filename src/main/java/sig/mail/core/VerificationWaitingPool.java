package sig.mail.core;

import java.util.Deque;
import java.util.LinkedList;

public class VerificationWaitingPool {

  private static final VerificationWaitingPool INSTANCE = new VerificationWaitingPool();

  private final Deque<String> messagesToVerify = new LinkedList<>();

  private VerificationWaitingPool() { }

  public void addToWaitingPool(String message) {
    synchronized (this) {
      messagesToVerify.add(message);
    }
  }

  public String geNextMessageToVerify() {
    synchronized (this) {
      return messagesToVerify.poll(); //will return null if empty
    }
  }

  public static VerificationWaitingPool getInstance() {
    return INSTANCE;
  }

}
