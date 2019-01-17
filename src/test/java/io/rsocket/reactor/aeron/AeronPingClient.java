package io.rsocket.reactor.aeron;

import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.util.ByteBufPayload;
import java.time.Duration;
import org.HdrHistogram.Recorder;
import reactor.aeron.AeronClient;
import reactor.aeron.AeronResources;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public final class AeronPingClient {

  public static void main(String... args) {
    AeronResources aeronResources = AeronResources.start();

    try {

      Mono<RSocket> client =
          RSocketFactory.connect()
              .frameDecoder(Frame::retain)
              .transport(
                  new AeronClientTransport(
                      AeronClient.create(aeronResources).options("localhost", 12000, 12001)))
              .start();

      PingClient pingClient = new PingClient(client);

      Recorder recorder = pingClient.startTracker(Duration.ofSeconds(1));

      int count = 1_000_000_000;

      pingClient
          .startPingPong(count, recorder)
          .doOnTerminate(() -> System.out.println("Sent " + count + " messages."))
          .blockLast();
    } finally {
      aeronResources.dispose();
      aeronResources.onDispose().block();
    }
  }

  private static class PingClient {

    private final Payload payload;
    private final Mono<RSocket> client;

    private PingClient(Mono<RSocket> client) {
      this.client = client;
      this.payload = ByteBufPayload.create("hello");
    }

    private Recorder startTracker(Duration interval) {
      final Recorder histogram = new Recorder(3600000000000L, 3);
      Flux.interval(interval)
          .doOnNext(
              aLong -> {
                System.out.println("---- PING/ PONG HISTO ----");
                histogram
                    .getIntervalHistogram()
                    .outputPercentileDistribution(System.out, 5, 1000.0, false);
                System.out.println("---- PING/ PONG HISTO ----");
              })
          .subscribe();
      return histogram;
    }

    private Flux<Payload> startPingPong(int count, final Recorder histogram) {
      return client
          .flatMapMany(
              rsocket ->
                  Flux.range(1, count)
                      .flatMap(
                          i -> {
                            long start = System.nanoTime();
                            return rsocket
                                .requestResponse(payload.retain())
                                .doOnNext(Payload::release)
                                .doFinally(
                                    signalType -> {
                                      long diff = System.nanoTime() - start;
                                      histogram.recordValue(diff);
                                    });
                          },
                          64))
          .doOnError(Throwable::printStackTrace);
    }
  }
}
