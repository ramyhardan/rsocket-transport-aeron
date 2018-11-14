package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoProcessor;
import reactor.ipc.aeron.AeronInbound;
import reactor.ipc.aeron.AeronOutbound;

public class AeronDuplexConnection implements DuplexConnection {

  private final MonoProcessor<Void> onClose;
  private final AeronInbound inbound;
  private final AeronOutbound outbound;
  private final EmitterProcessor<ByteBuffer> processor;
  private final Disposable disposable;

  public AeronDuplexConnection(AeronInbound inbound, AeronOutbound outbound) {
    this.processor = EmitterProcessor.create();
    this.inbound = inbound;
    this.outbound = outbound;
    this.onClose = MonoProcessor.create();

    // todo: onClose.doFinally(signalType -> { doSomething() }).subscribe();

    this.disposable = outbound.send(processor).then().subscribe(null, th -> {});
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
    return inbound
        .receive()
        .map(Unpooled::wrappedBuffer)
        .map(Frame::from)
        .log("DuplexConn receive -> ");
  }

  @Override
  public Mono<Void> onClose() {
    return onClose.log(" DuplexConn onClose ");
  }

  @Override
  public void dispose() {
    System.err.println("DuplexConn dispose ");
    if (!onClose.isDisposed()) {
      onClose.onComplete();
    }
  }

  @Override
  public boolean isDisposed() {
    return onClose.isDisposed();
  }
}
