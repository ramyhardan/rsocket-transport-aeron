package io.rsocket.reactor.aeron;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.aeron.client.AeronClient;
import reactor.ipc.aeron.client.AeronClientOptions;

public class AeronClientTransport implements ClientTransport {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronClientTransport.class);

  private final Consumer<AeronClientOptions> options;

  public AeronClientTransport(Consumer<AeronClientOptions> options) {
    this.options = options;
  }

  @Override
  public Mono<DuplexConnection> connect() {
    return Mono.create(
        sink -> {
          AeronClient client = AeronClient.create("client", options);
          client
              .newHandler(
                  (inbound, outbound) -> {
                    AeronDuplexConnection duplexConnection =
                        new AeronDuplexConnection(inbound, outbound);
                    LOGGER.info("{} connected", duplexConnection);
                    sink.success(duplexConnection);
                    return duplexConnection
                        .onClose()
                        .doOnSuccess(avoid -> LOGGER.info("{} closed", duplexConnection))
                        .doOnError(
                            th -> LOGGER.warn("{} closed with error: {}", duplexConnection, th))
                        .doOnTerminate(client::dispose);
                  })
              .subscribe(
                  null,
                  th -> {
                    LOGGER.warn("Failed to create client or connect duplexConnection: {}", th);
                    client.dispose();
                    sink.error(th);
                  });
        });
  }
}
