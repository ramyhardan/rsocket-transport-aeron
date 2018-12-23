package io.rsocket.reactor.aeron;

import io.aeron.CommonContext;
import io.aeron.driver.Configuration;
import io.rsocket.test.TransportTest;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import reactor.aeron.AeronResources;
import reactor.aeron.client.AeronClient;
import reactor.aeron.server.AeronServer;

class AeronTransportTest implements TransportTest {

  private static AtomicInteger portCounter = new AtomicInteger(12000);

  private static AeronResources aeronResources;

  private final TransportPair transportPair =
      new TransportPair<>(
          () -> InetSocketAddress.createUnresolved("localhost", portCounter.incrementAndGet()),
          (address, server) ->
              new AeronClientTransport(
                  AeronClient.create(aeronResources)
                      .options(
                          options -> {
                            options.serverChannel(Channels.from(address));
                            options.clientChannel(Channels.from(portCounter.incrementAndGet()));
                          })),
          (address) ->
              new AeronServerTransport(
                  AeronServer.create(aeronResources)
                      .options(options -> options.serverChannel(Channels.from(address)))));

  @BeforeAll
  static void beforeAll() {
    aeronResources = AeronResources.start();
  }

  @AfterAll
  static void afterAll() {
    if (aeronResources != null) {
      aeronResources.dispose();
      aeronResources.onDispose().block();
    }
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofSeconds(10);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
