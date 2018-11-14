package io.rsocket.reactor.aeron;

import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.transport.ServerTransport;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
          AeronServerWrapper aeronServerWrapper = new AeronServerWrapper(server);
          server
              .newHandler(
                  (in, out) -> {
                    DuplexConnection duplexConnection = new AeronDuplexConnection(in, out);

                    duplexConnection
                        .onClose()
                        .doOnTerminate(
                            () -> {
                              LOGGER.info(
                                  "{} was disposed, dispose {}",
                                  duplexConnection,
                                  aeronServerWrapper);
                              aeronServerWrapper.dispose();
                            })
                        .subscribe();

                    aeronServerWrapper
                        .onClose()
                        .doOnTerminate(
                            () -> {
                              LOGGER.info(
                                  "{} was disposed, dispose {}",
                                  aeronServerWrapper,
                                  duplexConnection);
                              duplexConnection.dispose();
                            })
                        .subscribe();

                    return acceptor
                        .apply(duplexConnection)
                        .doOnError(
                            th -> {
                              LOGGER.warn(
                                  "Rsocket processing of {} was finished with error: {}",
                                  duplexConnection,
                                  th);
                              aeronServerWrapper.dispose();
                            })
                        .then(duplexConnection.onClose());
                  })
              .subscribe(
                  avoid -> {
                    LOGGER.info("AeronServer was started");
                    sink.success(aeronServerWrapper);
                  },
                  th -> {
                    LOGGER.warn("Failed to create aeronServer: {}", th);
                    aeronServerWrapper.dispose();
                    sink.error(th);
                  });
        });
  }

  private static class AeronServerWrapper implements Closeable {

    @SuppressWarnings("unused")
    private final AeronServer server;

    private final MonoProcessor<Void> onClose = MonoProcessor.create();

    private AeronServerWrapper(AeronServer server) {
      this.server = server;
    }

    @Override
    public Mono<Void> onClose() {
      return onClose;
    }

    @Override
    public void dispose() {
      if (!onClose.isDisposed()) {
        onClose.onComplete();
      }
    }
  }
}
