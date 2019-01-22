package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.rsocket.Frame;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import reactor.aeron.DirectBufferHandler;

class FrameMapper implements DirectBufferHandler<Frame>, Function<DirectBuffer, Frame> {

  private final ByteBufAllocator byteBufAllocator = ByteBufAllocator.DEFAULT;
  private final DirectBufferHandler<ByteBuf> byteBufHandler = new ByteBufToDirectBufferHandler();

  @Override
  public int estimateLength(Frame frame) {
    return byteBufHandler.estimateLength(frame.content());
  }

  @Override
  public void write(MutableDirectBuffer destination, int offset, Frame frame, int length) {
    byteBufHandler.write(destination, offset, frame.content(), length);
  }

  @Override
  public DirectBuffer map(Frame frame, int length) {
    return byteBufHandler.map(frame.content(), length);
  }

  @Override
  public void dispose(Frame frame) {
    RefCountUtil.safestRelease(frame);
  }

  @Override
  public Frame apply(DirectBuffer source) {
    ByteBuf destination = byteBufAllocator.buffer(source.capacity());

    ByteBuf temp = Unpooled.wrappedBuffer(source.addressOffset(), source.capacity(), false);

    destination.writeBytes(temp, source.capacity());

    return Frame.from(destination);
  }
}
