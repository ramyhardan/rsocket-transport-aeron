package io.rsocket.reactor.aeron;

import io.rsocket.test.TransportTest;
import java.net.InetSocketAddress;
import java.time.Duration;
import org.junit.jupiter.api.Disabled;

@Disabled("add handling reactor.ipc.aeron.MessageType.COMPLETE")
class AeronTransportTest implements TransportTest {

  private final TransportPair transportPair =
      new TransportPair<>(
          () -> InetSocketAddress.createUnresolved("localhost", 13000),
          (address, server) -> new AeronClientTransport(options -> {
            options.serverChannel(Channels.from(address));
            options.clientChannel(Channels.clientChannel);
          }),
          (address) -> new AeronServerTransport(options -> {
            options.serverChannel(Channels.from(address));
          }));


  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(2);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}