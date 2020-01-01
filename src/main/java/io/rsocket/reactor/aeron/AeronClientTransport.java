package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.DuplexConnection;
import io.rsocket.fragmentation.FragmentationDuplexConnection;
import io.rsocket.transport.ClientTransport;
import reactor.aeron.mdc.AeronClient;
import reactor.core.publisher.Mono;

public class AeronClientTransport implements ClientTransport {

  private final AeronClient client;

  public AeronClientTransport(AeronClient client) {
    this.client = client;
  }

  @Override
  public Mono<DuplexConnection> connect(int mtu) {
    Mono<DuplexConnection> isError = FragmentationDuplexConnection.checkMtu(mtu);
    return isError != null
        ? isError
        : client
            // .doOnConnected(c -> c.addHandlerLast(new RSocketLengthCodec()))
            .connect()
            .map(
                c -> {
                  if (mtu > 0) {
                    return new FragmentationDuplexConnection(
                        new AeronDuplexConnection(
                            c, new FrameMapper(ByteBufAllocator.DEFAULT, false)),
                        ByteBufAllocator.DEFAULT,
                        mtu,
                        true,
                        "client");
                  } else {
                    return new AeronDuplexConnection(c, new FrameMapper(ByteBufAllocator.DEFAULT));
                  }
                });
  }
}
