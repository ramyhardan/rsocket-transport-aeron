package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Closeable;
import io.rsocket.DuplexConnection;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ServerTransport;
import java.util.Objects;
import reactor.aeron.OnDisposable;
import reactor.aeron.mdc.AeronServer;
import reactor.core.publisher.Mono;

public class AeronServerTransport implements ServerTransport<Closeable> {

  private final AeronServer server;

  public AeronServerTransport(AeronServer server) {
    this.server = server;
  }

  @Override
  public Mono<Closeable> start(ConnectionAcceptor acceptor, int mtu) {
    Objects.requireNonNull(acceptor, "acceptor must not be null");
    Mono<Closeable> isError = FragmentationDuplexConnection.checkMtu(mtu);
    return isError != null
        ? isError
        : server
            .handle(
                c -> {
                  // c.addHandlerLast(new RSocketLengthCodec());
                  DuplexConnection connection;
                  if (mtu > 0) {
                    connection =
                        new FragmentationDuplexConnection(
                            new AeronDuplexConnection(
                                c, new FrameMapper(ByteBufAllocator.DEFAULT, false)),
                            ByteBufAllocator.DEFAULT,
                            mtu,
                            true,
                            "server");
                  } else {
                    connection =
                        new AeronDuplexConnection(
                            c, new FrameMapper(ByteBufAllocator.DEFAULT));
                  }
                  acceptor
                      .apply(connection)
                      .then(Mono.<Void>never())
                      .subscribe(c.disposeSubscriber());
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
