package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.aeron.Connection;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AeronDuplexConnection implements DuplexConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronDuplexConnection.class);

  private final EmitterProcessor<ByteBuffer> processor = EmitterProcessor.create();
  private final Connection connection;
  private final Disposable outboundDisposable;

  public AeronDuplexConnection(Connection connection) {
    this.connection = connection;
    this.outboundDisposable =
        connection
            .outbound()
            .send(processor)
            .then()
            .subscribe(
                null,
                th -> {
                  LOGGER.info("outbound of {} was failed with error: {}", this, th);
                  dispose();
                },
                this::dispose);
  }

  @Override
  public Mono<Void> send(Publisher<Frame> frames) {
    return Mono.create(
        sink ->
            Flux.from(frames)
                .log("DuplexConn send -> ")
                .map(Frame::content)
                .map(ByteBuf::nioBuffer)
                .subscribe(processor::onNext, sink::error, sink::success));
  }

  @Override
  public Mono<Void> sendOne(Frame frame) {
    return send(Mono.just(frame));
  }

  @Override
  public Flux<Frame> receive() {
    return connection.inbound().receive().map(Unpooled::wrappedBuffer).map(Frame::from);
  }

  @Override
  public Mono<Void> onClose() {
    return connection.onDispose();
  }

  @Override
  public void dispose() {
    outboundDisposable.dispose();
    connection.dispose();
  }

  @Override
  public boolean isDisposed() {
    return connection.isDisposed();
  }
}
