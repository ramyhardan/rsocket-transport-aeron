package io.rsocket.reactor.aeron;

import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.util.ByteBufPayload;
import java.util.concurrent.ThreadLocalRandom;
import reactor.aeron.AeronResources;
import reactor.aeron.AeronServer;
import reactor.core.publisher.Mono;

public final class AeronPongServer {

  public static void main(String... args) {
    AeronResources aeronResources = AeronResources.start();

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
