package io.rsocket.reactor.aeron;

import io.rsocket.test.TransportTest;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import reactor.aeron.AeronResources;
import reactor.aeron.AeronResourcesConfig;
import reactor.aeron.client.AeronClient;
import reactor.aeron.server.AeronServer;

class AeronTransportTest implements TransportTest {

  private static AtomicInteger portCounter = new AtomicInteger(12000);

  private static AeronResources serverAeronResources;
  private static AeronResources clientAeronResources;

  private final TransportPair transportPair =
      new TransportPair<>(
          () -> InetSocketAddress.createUnresolved("localhost", portCounter.addAndGet(2)),
          (address, server) ->
              new AeronClientTransport(
                  AeronClient.create(clientAeronResources)
                      .options(address.getHostString(), address.getPort(), address.getPort() + 1)),
          (address) ->
              new AeronServerTransport(
                  AeronServer.create(serverAeronResources)
                      .options(address.getHostString(), address.getPort(), address.getPort() + 1)));

  @BeforeAll
  static void beforeAll() {
    serverAeronResources =
        AeronResources.start(AeronResourcesConfig.builder().numOfWorkers(1).build());
    clientAeronResources =
        AeronResources.start(AeronResourcesConfig.builder().numOfWorkers(1).build());
  }

  @AfterAll
  static void afterAll() {
    if (serverAeronResources != null) {
      serverAeronResources.dispose();
      serverAeronResources.onDispose().block();
    }
    if (clientAeronResources != null) {
      clientAeronResources.dispose();
      clientAeronResources.onDispose().block();
    }
  }

  @Override
  public Duration getTimeout() {
    return Duration.ofMinutes(1);
  }

  @Override
  public TransportPair getTransportPair() {
    return transportPair;
  }
}
