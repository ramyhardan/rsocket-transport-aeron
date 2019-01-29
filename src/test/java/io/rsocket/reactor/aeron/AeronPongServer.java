package io.rsocket.reactor.aeron;

import io.aeron.driver.ThreadingMode;
import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.ByteBufPayload;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import reactor.aeron.AeronResources;
import reactor.aeron.AeronServer;
import reactor.core.publisher.Mono;

public final class AeronPongServer {

  public static void main(String... args) {

    Supplier<IdleStrategy> idleStrategySupplier = () -> new BackoffIdleStrategy(1, 1, 1, 100);

    AeronResources aeronResources =
        new AeronResources()
            .useTmpDir()
            .pollFragmentLimit(32)
            .writeLimit(32)
            .singleWorker()
            .media(
                ctx ->
                    ctx.threadingMode(ThreadingMode.DEDICATED)
                        .conductorIdleStrategy(idleStrategySupplier.get())
                        .receiverIdleStrategy(idleStrategySupplier.get())
                        .senderIdleStrategy(idleStrategySupplier.get())
                        .termBufferSparseFile(false))
            .start()
            .block();

    try {
      RSocketFactory.receive()
          .frameDecoder(Frame::retain)
          .acceptor(new PingHandler())
          .transport(
              new AeronServerTransport(
                  AeronServer.create(aeronResources).options("localhost", 12000, 12001)))
          .start()
          .block()
          .onClose()
          .block();

    } finally {
      aeronResources.dispose();
      aeronResources.onDispose().block();
    }
  }

  private static class PingHandler implements SocketAcceptor {

    private final Payload pong;

    private PingHandler() {
      byte[] data = new byte[1024];
      ThreadLocalRandom.current().nextBytes(data);
      pong = ByteBufPayload.create(data);
    }

    @Override
    public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
      // setup.release();
      return Mono.just(
          new AbstractRSocket() {
            @Override
            public Mono<Payload> requestResponse(Payload payload) {
              payload.release();
              return Mono.just(pong.retain());
            }
          });
    }
  }
}
