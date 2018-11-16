package io.rsocket.reactor.aeron;

import io.rsocket.DuplexConnection;
import io.rsocket.transport.ClientTransport;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.ipc.aeron.client.AeronClient;
import reactor.ipc.aeron.client.AeronClientOptions;

public class AeronClientTransport implements ClientTransport {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronClientTransport.class);

  private final AeronClient client;

  public AeronClientTransport(Consumer<AeronClientOptions> options) {
    this.client = AeronClient.create("client", options);
  }

  @Override
  public Mono<DuplexConnection> connect() {
    return Mono.create(
        sink -> {
          AtomicReference<AeronDuplexConnection> connectionReference = new AtomicReference<>();
          client
              .newHandler(
                  (in, out) -> {
                    AeronDuplexConnection connection = new AeronDuplexConnection(in, out);
                    connectionReference.set(connection);
                    LOGGER.info("{} connected", connection);
                    sink.success(connection);
                    return connection
                        .onClose()
                        .doOnSuccess(avoid -> LOGGER.info("{} closed", connection))
                        .doOnError(th -> LOGGER.warn("{} closed with error: {}", connection, th));
                  })
              .subscribe(
                  clientHandler -> {
                    AeronDuplexConnection connection = connectionReference.get();
                    connection
                        .onClose()
                        .doOnTerminate(clientHandler::dispose)
                        .subscribe(
                            null,
                            th -> LOGGER.warn("{} disposed with error: {}", clientHandler, th));
                  },
                  th -> {
                    LOGGER.warn("Failed to create client or connect connection: {}", th);
                    sink.error(th);
                  });
        });
  }
}
