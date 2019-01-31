package io.rsocket.reactor.aeron;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Frame;
import java.util.function.Function;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import reactor.aeron.DirectBufferHandler;

class FrameMapper implements DirectBufferHandler<Frame>, Function<DirectBuffer, Frame> {

  @Override
  public int estimateLength(Frame frame) {
    return frame.content().readableBytes();
  }

  @Override
  public void write(MutableDirectBuffer dstBuffer, int offset, Frame frame, int length) {
    UnsafeBuffer srcBuffer = new UnsafeBuffer(frame.content().memoryAddress(), length);
    dstBuffer.putBytes(offset, srcBuffer, 0, length);
  }

  @Override
  public DirectBuffer map(Frame frame, int length) {
    return new UnsafeBuffer(frame.content().memoryAddress(), length);
  }

  @Override
  public void dispose(Frame frame) {
    RefCountUtil.safestRelease(frame);
  }

  @Override
  public Frame apply(DirectBuffer source) {
    ByteBuf destination = ByteBufAllocator.DEFAULT.buffer(source.capacity());
    source.getBytes(0, destination.internalNioBuffer(0, source.capacity()), source.capacity());
    return Frame.from(destination);
  }
}
