package io.rsocket.reactor.aeron;

import io.rsocket.DuplexConnection;
import io.rsocket.Frame;
import org.reactivestreams.Publisher;
import reactor.aeron.AeronConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class AeronDuplexConnection implements DuplexConnection {

  private final AeronConnection connection;
  private final FrameMapper frameMapper = new FrameMapper();

  public AeronDuplexConnection(AeronConnection connection) {
    this.connection = connection;
  }

  @Override
  public Mono<Void> send(Publisher<Frame> frames) {
    return connection.outbound().send(Flux.from(frames), frameMapper).then();
  }

  @Override
  public Mono<Void> sendOne(Frame frame) {
    return connection.outbound().send(Mono.just(frame), frameMapper).then();
  }

  @Override
  public Flux<Frame> receive() {
    return connection.inbound().receive().map(frameMapper);
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
}
