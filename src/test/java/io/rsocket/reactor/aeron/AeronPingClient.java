package io.rsocket.reactor.aeron;

import io.aeron.driver.ThreadingMode;
import io.rsocket.Frame;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.util.ByteBufPayload;
import java.time.Duration;
import org.HdrHistogram.Recorder;
import reactor.aeron.AeronClient;
import reactor.aeron.AeronResources;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

public final class AeronPingClient {

  public static void main(String... args) {
    int count = 1_000_000_000;

    AeronResources aeronResources =
        new AeronResources()
            .useTmpDir()
            .media(mdc -> mdc.threadingMode(ThreadingMode.SHARED))
            .start()
            .block();

    try {
      RSocketFactory.connect()
          .frameDecoder(Frame::retain)
          .transport(
              () ->
                  new AeronClientTransport(
                      AeronClient.create(aeronResources).options("localhost", 12000, 12001)))
          .start()
          .log("client connect() ")
          .flatMapMany(
              rsocket -> {
                PingClient pingClient = new PingClient();
                return pingClient.startPingPong(rsocket, count).doFinally(s -> pingClient.close());
              })
          .doOnTerminate(() -> System.out.println("Sent " + count + " messages."))
          .blockLast();

    } finally {
      aeronResources.dispose();
      aeronResources.onDispose().block();
    }
  }

  private static class PingClient {

    private static final Duration REPORT_INTERVAL = Duration.ofSeconds(1);

    private final Recorder histogram;

    private final Payload payload;
    private final Disposable reportDisposable;

    private PingClient() {
      this.payload = ByteBufPayload.create("hello");

      this.histogram = new Recorder(3600000000000L, 3);
      this.reportDisposable =
          Flux.interval(REPORT_INTERVAL)
              .doOnNext(
                  aLong -> {
                    System.out.println("---- PING/ PONG HISTO ----");
                    histogram
                        .getIntervalHistogram()
                        .outputPercentileDistribution(System.out, 5, 1000.0, false);
                    System.out.println("---- PING/ PONG HISTO ----");
                  })
              .subscribe();
    }

    private void close() {
      reportDisposable.dispose();
    }

    private Flux<Payload> startPingPong(RSocket rsocket, int count) {
      return Flux.range(1, count)
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
              64)
          .doOnError(Throwable::printStackTrace);
    }
  }
}
