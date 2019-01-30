package io.rsocket.reactor.aeron;

import static io.rsocket.test.TransportTest.TIMEOUT;

import io.rsocket.Closeable;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.test.TestRSocket;
import io.rsocket.util.ByteBufPayload;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.aeron.AeronClient;
import reactor.aeron.AeronResources;
import reactor.aeron.AeronServer;
import reactor.core.publisher.Mono;

public class SimpleTest {

  private static AeronResources resources;
  private static RSocket client;
  private static Closeable server;

  @BeforeAll
  static void setUp() {
    resources = new AeronResources().useTmpDir().singleWorker().start().block();

    server =
        RSocketFactory.receive()
            .acceptor((setup, sendingSocket) -> Mono.just(new TestRSocket()))
            .transport(
                new AeronServerTransport(
                    AeronServer.create(SimpleTest.resources).options("localhost", 14000, 14001)))
            .start().log("dasd ")
            .block(TIMEOUT);

    client =
        RSocketFactory.connect()
            .transport(
                new AeronClientTransport(
                    AeronClient.create(SimpleTest.resources).options("localhost", 14000, 14001)))
            .start()
            .doOnError(Throwable::printStackTrace)
            .block(TIMEOUT);
  }

  @AfterAll
  static void tearDown() {
    client.dispose();
    server.dispose();
    resources.dispose();
  }

  @Test
  void name() {

    String response =
        client
            .requestStream(ByteBufPayload.create("hello world", "metadata"))
            .map(Payload::getDataUtf8)
            .doOnNext(str -> System.out.println(str))
            .blockLast();

    System.out.println(response);
  }
}
