package io.rsocket.reactor.aeron;

import io.rsocket.Closeable;
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
    return Mono.defer(() -> new AeronServerWrapper(acceptor, aeronOptions).start());
  }

  private static class AeronServerWrapper implements Closeable {

    @SuppressWarnings("unused")
    private final AeronServer server;

    private final ConnectionAcceptor acceptor;
    private final Consumer<AeronOptions> aeronOptions;
    private final MonoProcessor<Void> onClose;

    private AeronServerWrapper(ConnectionAcceptor acceptor, Consumer<AeronOptions> aeronOptions) {
      this(null, acceptor, aeronOptions, MonoProcessor.create());
    }

    private AeronServerWrapper(AeronServer server, AeronServerWrapper other) {
      this(server, other.acceptor, other.aeronOptions, other.onClose);
    }

    private AeronServerWrapper(
        AeronServer server,
        ConnectionAcceptor acceptor,
        Consumer<AeronOptions> aeronOptions,
        MonoProcessor<Void> onClose) {
      this.server = server;
      this.acceptor = acceptor;
      this.aeronOptions = aeronOptions;
      this.onClose = onClose;
    }

    private Mono<AeronServerWrapper> start() {
      return Mono.create(
          sink -> {
            AeronServer server = AeronServer.create("server", aeronOptions);
            server
                .newHandler(
                    (inbound, outbound) -> {
                      AeronDuplexConnection duplexConnection =
                          new AeronDuplexConnection(inbound, outbound);
                      onClose.doOnTerminate(
                          () -> {
                            System.err.println(
                                "AeronServerWrapper duplexConnection.dispose()");
                            duplexConnection.dispose();
                          }); //todo
                      return acceptor
                          .apply(duplexConnection)
                          .doOnError(th -> onClose.onComplete()) // todo
                          .doOnCancel(() -> onClose.onComplete()) //todo
                          .doOnSuccess(avoid -> {

                          })
                          .then(duplexConnection.onClose());
                    })
                .subscribe(
                    avoid -> sink.success(new AeronServerWrapper(server, this)),
                    th -> {
                      LOGGER.warn("Failed to create aeronServer: {}", th);
                      sink.error(th);
                    });
          });
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
