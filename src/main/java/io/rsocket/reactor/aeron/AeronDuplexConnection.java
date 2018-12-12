package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.aeron.Connection;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AeronDuplexConnection implements DuplexConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronDuplexConnection.class);

  private final Connection connection;
  private final Disposable channelClosed;

  public AeronDuplexConnection(Connection connection) {
    this.connection = connection;
    channelClosed =
        connection
            .onDispose()
            .doFinally(
                s -> {
                  if (!isDisposed()) {
                    dispose();
                  }
                })
            .subscribe();
  }

  @Override
  public Mono<Void> send(Publisher<Frame> frames) {
    return Flux.from(frames)
        .map(
            frame -> {
              ByteBuffer buffer = frame.content().nioBuffer();
              ReferenceCountUtil.safeRelease(frame);
              return buffer;
            })
        .flatMap(buffer -> connection.outbound().send(Mono.just(buffer)).then())
        .then();
  }

  @Override
  public Flux<Frame> receive() {
    return connection
        .inbound()
        .receive()
        .map(
            buffer -> {
              ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(buffer.capacity());
              byteBuf.writeBytes(buffer);
              return byteBuf;
            })
        .map(Frame::from);
  }

  @Override
  public Mono<Void> onClose() {
    return connection
        .onDispose()
        .doFinally(
            s -> {
              if (!channelClosed.isDisposed()) {
                channelClosed.dispose();
              }
            });
  }

  @Override
  public void dispose() {
    connection.dispose();
  }

  @Override
  public boolean isDisposed() {
    return connection.isDisposed();
  }
}
