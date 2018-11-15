package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.aeron.AeronInbound;
import reactor.ipc.aeron.AeronOutbound;
import reactor.ipc.aeron.ByteBufferFlux;

public class AeronDuplexConnection implements DuplexConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(AeronDuplexConnection.class);

  private final EmitterProcessor<ByteBuffer> processor = EmitterProcessor.create();
  private final MonoProcessor<Void> onClose = MonoProcessor.create();

  private final ByteBufferFlux inbound;
  private final Disposable outboundDisposable;

  public AeronDuplexConnection(AeronInbound inbound, AeronOutbound outbound) {
    this.inbound = inbound.receive();
    this.outboundDisposable =
        outbound
            .send(processor)
            .then()
            .subscribe(
                null,
                th -> {
                  LOGGER.info("outbound of {} was failed with error: {}", this, th);
                  dispose();
                },
                this::dispose);

    // todo we need to subscribe to Connection(io/out).onClose().doOnTerminate(() -> dispose()) to
    // dispose itself
    onClose.doOnTerminate(this::dispose).subscribe();
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
    return inbound.map(Unpooled::wrappedBuffer).map(Frame::from);
  }

  @Override
  public Mono<Void> onClose() {
    return onClose;
  }

  @Override
  public void dispose() {
    System.err.println("DUPLEX_CONN_DISPOSE");
    if (!onClose.isDisposed()) {
      onClose.onComplete();
    }
    if (!outboundDisposable.isDisposed()) {
      outboundDisposable.dispose();
    }

    // todo inbound.dispose();
  }

  @Override
  public boolean isDisposed() {
    return onClose.isDisposed();
  }
}
