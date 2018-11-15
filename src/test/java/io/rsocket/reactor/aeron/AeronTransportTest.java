package io.rsocket.reactor.aeron;

import io.rsocket.test.TransportTest;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Disabled;

@Disabled("add handling reactor.ipc.aeron.MessageType.COMPLETE")
class AeronTransportTest implements TransportTest {

  private static AtomicInteger portCounter = new AtomicInteger(12000);

  private final TransportPair transportPair =
      new TransportPair<>(
          () -> InetSocketAddress.createUnresolved("localhost", portCounter.incrementAndGet()),
          (address, server) ->
              new AeronClientTransport(
                  options -> {
                    options.serverChannel(Channels.from(address));
                    options.clientChannel(Channels.from(portCounter.incrementAndGet()));
                  }),
          (address) ->
              new AeronServerTransport(options -> options.serverChannel(Channels.from(address))));

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(2);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
