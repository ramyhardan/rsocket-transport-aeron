package io.rsocket.reactor.aeron;

import static io.aeron.driver.Configuration.TERM_BUFFER_LENGTH_DEFAULT;

import io.rsocket.test.TransportTest;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import reactor.aeron.AeronResources;
import reactor.aeron.client.AeronClient;
import reactor.aeron.server.AeronServer;

class AeronTransportTest implements TransportTest {

  private static AtomicInteger portCounter = new AtomicInteger(12000);

  static {
    // fixme workaround wait for backpressure support
    System.setProperty("aeron.term.buffer.length", String.valueOf(2 * TERM_BUFFER_LENGTH_DEFAULT));
  }

  private volatile AeronResources aeronResources;

  private final TransportPair transportPair =
      new TransportPair<>(
          () -> InetSocketAddress.createUnresolved("localhost", portCounter.incrementAndGet()),
          (address, server) -> {
            if (aeronResources == null) {
              aeronResources = AeronResources.start();
            }
            return new AeronClientTransport(
                AeronClient.create(aeronResources)
                    .options(
                        options -> {
                          options.serverChannel(Channels.from(address));
                          options.clientChannel(Channels.from(portCounter.incrementAndGet()));
                        }));
          },
          (address) -> {
            if (aeronResources == null) {
              aeronResources = AeronResources.start();
            }
            return new AeronServerTransport(
                AeronServer.create(aeronResources)
                    .options(options -> options.serverChannel(Channels.from(address))));
          });

  @AfterEach
  void tearDown() {
    if (aeronResources != null) {
      aeronResources.close();
    }
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(10);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
