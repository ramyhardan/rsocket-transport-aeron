package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import reactor.aeron.AeronConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AeronDuplexConnection implements DuplexConnection {

  private final AeronConnection connection;

  public AeronDuplexConnection(AeronConnection connection) {
    this.connection = connection;
  }

  @Override
  public Mono<Void> send(Publisher<Frame> frames) {
    return connection.outbound().send(Flux.from(frames).map(this::toByteBuffer)).then();
  }

  @Override
  public Mono<Void> sendOne(Frame frame) {
    return connection.outbound().send(Mono.just(frame).map(this::toByteBuffer)).then();
  }

  @Override
  public Flux<Frame> receive() {
    return connection.inbound().receive().map(this::toFrame);
  }

  @Override
  public Mono<Void> onClose() {
    return connection.onDispose();
  }

  @Override
  public void dispose() {
    connection.dispose();
  }

  @Override
  public boolean isDisposed() {
    return connection.isDisposed();
  }

  private ByteBuffer toByteBuffer(Frame frame) {
    ByteBuffer buffer = frame.content().nioBuffer();
    ByteBuffer bufferCopy = ByteBuffer.allocate(buffer.remaining());
    bufferCopy.put(buffer);
    bufferCopy.flip();
    ReferenceCountUtil.safeRelease(frame);
    return bufferCopy;
  }

  private Frame toFrame(ByteBuffer buffer) {
    ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(buffer.remaining());
    byteBuf.writeBytes(buffer);
    return Frame.from(byteBuf);
  }
}
