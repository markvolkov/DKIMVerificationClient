package sig.mail.core;

import com.sun.mail.imap.protocol.IMAPResponse;
import com.yahoo.imapnio.async.client.ImapAsyncClient;
import com.yahoo.imapnio.async.client.ImapAsyncCreateSessionResponse;
import com.yahoo.imapnio.async.client.ImapAsyncSession;
import com.yahoo.imapnio.async.client.ImapAsyncSession.DebugMode;
import com.yahoo.imapnio.async.client.ImapAsyncSessionConfig;
import com.yahoo.imapnio.async.client.ImapFuture;
import com.yahoo.imapnio.async.data.MessageNumberSet;
import com.yahoo.imapnio.async.exception.ImapAsyncClientException;
import com.yahoo.imapnio.async.request.FetchCommand;
import com.yahoo.imapnio.async.request.LoginCommand;
import com.yahoo.imapnio.async.request.SelectFolderCommand;
import com.yahoo.imapnio.async.response.ImapAsyncResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLException;
import lombok.SneakyThrows;

public class MessageProducer implements Runnable {

  private static final String IMAP_SERVER_URI = "imaps://imap.gmail.com:993";
  private static final Logger LOGGER = Logger.getLogger(MessageProducer.class.getName());

  private ImapAsyncSession imapAsyncSession;
  private final AtomicInteger atomicSequenceNumber;
  private volatile int lastSequenceNumber;

  public MessageProducer(int numOfClientThreads) {
    prepareImapConnection(numOfClientThreads);
    this.atomicSequenceNumber = new AtomicInteger(1800); //The message number set will be grabbed on startup and stored in a monotonically increasing variable
  }

  private void prepareImapConnection(final int numOfClientThreads) {
    ImapAsyncClient imapClient = null;
    try {
      imapClient = new ImapAsyncClient(numOfClientThreads);
    } catch (SSLException e) {
      LOGGER.log(Level.INFO, "Error creating imap client!", e);
    }
    URI serverUri = null;
    try {
      serverUri = new URI(IMAP_SERVER_URI);
    } catch (URISyntaxException e) {
      LOGGER.log(Level.INFO, "Error creating server uri!", e);
    }
    boolean readyToAttemptConnection = imapClient != null && serverUri != null;
    if (!readyToAttemptConnection) {
      LOGGER.log(Level.INFO, "ImapClient or ServerUri failed instantiation!");
      System.exit(-1); //need to fail or retry a certain amount of times
    }
    final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
    config.setConnectionTimeoutMillis(5000);
    config.setReadTimeoutMillis(6000);
    final ImapFuture<ImapAsyncCreateSessionResponse> future = (ImapFuture<ImapAsyncCreateSessionResponse>) imapClient
        .createSession(serverUri, config, null, null, DebugMode.DEBUG_OFF);
    future.setDoneCallback(sessionResponse -> {
      ImapAsyncSession session = sessionResponse.getSession();
      try {
        session.execute(new LoginCommand("your.email@gmail.com", "yourpassword"))
            .setDoneCallback(imapAsyncResponse -> {
              LOGGER.info("Logged in!");
              try {
                ImapFuture<ImapAsyncResponse> inboxFuture = session
                    .execute(new SelectFolderCommand("inbox"));
                inboxFuture.setDoneCallback(done -> {
                  LOGGER.info("Got inbox!");
                  imapAsyncSession = session;
                });
              } catch (ImapAsyncClientException e) {
                LOGGER.log(Level.INFO, "Error getting inbox!", e);
              }
            });
      } catch (ImapAsyncClientException e) {
        LOGGER.log(Level.INFO, "Error logging in to imap client!", e);
      }
    });
    future.setExceptionCallback(exception -> {
      LOGGER.log(Level.INFO, "Error creating imap session", exception);
    });
  }

  @SneakyThrows //remove and handle error
  @Override
  public void run() {
    if (imapAsyncSession == null) {
      return;
    }
    final int sequenceNumber = atomicSequenceNumber.get();
    if (sequenceNumber >= 2049) {
      System.exit(0); //just for testing going to be removed
    }
    if (sequenceNumber == lastSequenceNumber) {
      return;
    }
    lastSequenceNumber = sequenceNumber;
    try {
      imapAsyncSession.execute(new FetchCommand(
          new MessageNumberSet[]{new MessageNumberSet(sequenceNumber, sequenceNumber)},
          "RFC822.HEADER RFC822.TEXT")).setDoneCallback(fetch -> {
        for (IMAPResponse fetched : fetch.getResponseLines()) {
          String fetchRes = fetched.toString();
          VerificationWaitingPool.getInstance().addToWaitingPool(fetchRes);
        }
        atomicSequenceNumber.getAndIncrement();
      });
    } catch (ImapAsyncClientException e) {
      LOGGER.log(Level.INFO, "Error getting msg for sequence number: " + sequenceNumber, e);
    }
  }

}
