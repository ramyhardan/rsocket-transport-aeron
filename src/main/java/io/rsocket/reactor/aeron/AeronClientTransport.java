package io.rsocket.reactor.aeron;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import reactor.core.publisher.Mono;
import reactor.ipc.aeron.client.AeronClient;
import reactor.ipc.aeron.client.AeronClientOptions;

public class AeronClientTransport implements ClientTransport {

  private final Consumer<AeronClientOptions> options;

  public AeronClientTransport(Consumer<AeronClientOptions> options) {
    this.options = options;
  }

  @Override
  public Mono<DuplexConnection> connect() {
    return Mono.<DuplexConnection>create(
            sink -> {
              AeronClient client = AeronClient.create("client", options);
              AtomicReference<AeronDuplexConnection> holder = new AtomicReference<>(); // todo
              client
                  .newHandler(
                      (inbound, outbound) -> {
                        AeronDuplexConnection duplexConnection =
                            new AeronDuplexConnection(inbound, outbound);
                        holder.set(duplexConnection);
                        return duplexConnection.onClose();
                      })
                  .doOnSuccess(
                      disposable -> {
                        AeronDuplexConnection duplexConnection = holder.get();
                        sink.success(duplexConnection);

                        // todo for test (before sink - it works!!!!!)
                        // duplexConnection.sendOne(Frame.Setup.from(0, 0, 0, "", "",
                        // EmptyPayload.INSTANCE))
                        //     .then()
                        //     .log("raw test send5 ")
                        //     .subscribeOn(Schedulers.single())
                        //     .subscribe();

                      })
                  .subscribe();
            })
        .log("AeronClientTransport connect ");
  }
}
