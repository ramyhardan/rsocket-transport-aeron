package io.rsocket.reactor.aeron;

import io.rsocket.Payload;
import io.rsocket.RSocketFactory;
import io.rsocket.util.ByteBufPayload;
import reactor.aeron.AeronResources;
import reactor.aeron.client.AeronClient;
import reactor.core.publisher.Flux;

public class RsocketClientRunner {

  public static void main(String[] args) throws Exception {

    AeronResources aeronResources = AeronResources.start();
    try {

      // start client
      RSocketFactory.connect()
          .transport(
              () ->
                  new AeronClientTransport(
                      AeronClient.create(aeronResources).options("localhost", 12000, 12001)))
          .start()
          .log("client connect() ")
          .subscribe(
              rSocket -> {
                System.err.println("start " + rSocket);

                Flux.range(0, 1000)
                    .flatMap(
                        i ->
                            rSocket
                                .requestResponse(ByteBufPayload.create("Hello_" + i))
                                .log("receive ")
                                .map(Payload::getDataUtf8)
                                .doOnNext(System.out::println)
                                .then())
                    .doFinally(s -> rSocket.dispose())
                    .subscribe();
              });

      System.err.println("wait for the end");
      Thread.currentThread().join();
    } finally {
      aeronResources.dispose();
      aeronResources.onDispose().block();
    }
  }
}
