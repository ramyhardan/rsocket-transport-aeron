package io.rsocket.reactor.aeron;

import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ServerTransport;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.aeron.AeronOptions;
import reactor.ipc.aeron.server.AeronServer;

public class AeronServerTransport implements ServerTransport<Closeable> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronServerTransport.class);

  private final Consumer<AeronOptions> aeronOptions;

  public AeronServerTransport(Consumer<AeronOptions> aeronOptions) {
    this.aeronOptions = aeronOptions;
  }

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor) {
    return Mono.create(
        sink -> {
          AeronServer server = AeronServer.create("server", aeronOptions);
          server
              .newHandler(
                  (in, out) -> {
                    DuplexConnection duplexConnection = new AeronDuplexConnection(in, out);
                    return acceptor
                        .apply(duplexConnection)
                        .doOnError(
                            th -> {
                              LOGGER.error(
                                  "Acceptor didn't apply {}, reason: {}", duplexConnection, th);
                              duplexConnection.dispose();
                            })
                        .then(duplexConnection.onClose());
                  })
              .subscribe(
                  serverHandler -> {
                    LOGGER.info("AeronServer started");
                    sink.success(new AeronServerWrapper(serverHandler));
                  },
                  th -> {
                    LOGGER.error("Failed to create aeronServer: {}", th);
                    sink.error(th);
                  });
        });
  }

  private static class AeronServerWrapper implements Closeable {

    private final Disposable serverHandler;
    private final MonoProcessor<Void> onClose = MonoProcessor.create();

    private AeronServerWrapper(Disposable serverHandler) {
      this.serverHandler = serverHandler;
    }

    @Override
    public Mono<Void> onClose() {
      return onClose;
    }

    @Override
    public void dispose() {
      if (!isDisposed()) {
        onClose.onComplete();
      }
      if (!serverHandler.isDisposed()) {
        serverHandler.dispose();
      }
    }

    @Override
    public boolean isDisposed() {
      return onClose.isDisposed();
    }
  }
}
