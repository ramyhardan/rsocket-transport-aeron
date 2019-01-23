package io.rsocket.reactor.aeron;

import io.netty.buffer.Unpooled;
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
    // TODO must be changed to get rid of Unpooled
    return Frame.from(Unpooled.wrappedBuffer(source.addressOffset(), source.capacity(), false));
  }
}
