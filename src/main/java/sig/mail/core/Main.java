package sig.mail.core;

public class Main {

  public static void main(String[] args) {
    final MessageProducer messageProducer = new MessageProducer(2);
    final MessageConsumer messageConsumer = new MessageConsumer();
    while (true) {
      messageProducer.run();
      messageConsumer.run();
    }
  }

}
