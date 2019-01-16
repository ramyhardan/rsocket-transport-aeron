package io.rsocket.reactor.aeron;

import io.rsocket.Closeable;
import io.rsocket.transport.ServerTransport;
import reactor.aeron.OnDisposable;
import reactor.aeron.AeronServer;
import reactor.core.publisher.Mono;

public class AeronServerTransport implements ServerTransport<Closeable> {

  private final AeronServer server;

  public AeronServerTransport(AeronServer server) {
    this.server = server;
  }

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor) {
    return server
        .handle(
            c -> {
              AeronDuplexConnection connection = new AeronDuplexConnection(c);
              acceptor.apply(connection).then(Mono.<Void>never()).subscribe(c.disposeSubscriber());
              return c.onDispose();
            })
        .bind()
        .map(AeronServerWrapper::new);
  }

  private static class AeronServerWrapper implements Closeable {

    private final OnDisposable serverHandler;

    private AeronServerWrapper(OnDisposable serverHandler) {
      this.serverHandler = serverHandler;
    }

    @Override
    public Mono<Void> onClose() {
      return serverHandler.onDispose();
    }

    @Override
    public void dispose() {
      serverHandler.dispose();
    }

    @Override
    public boolean isDisposed() {
      return serverHandler.isDisposed();
    }
  }
}
